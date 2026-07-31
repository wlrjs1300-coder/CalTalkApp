package com.caltalk.backend.schedule;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.common.error.InvalidScheduleTimeRangeException;
import com.caltalk.backend.common.error.InvalidScheduleUpdateException;
import com.caltalk.backend.common.error.InvalidScheduleQueryRangeException;
import com.caltalk.backend.common.error.ScheduleConflictException;
import com.caltalk.backend.common.error.ScheduleNotFoundException;
import com.caltalk.backend.common.error.ScheduleVersionConflictException;
import com.caltalk.backend.confirmation.ConfirmationService;
import com.caltalk.backend.schedule.history.ScheduleChangeHistory;
import com.caltalk.backend.schedule.history.ScheduleChangeHistoryRepository;
import com.caltalk.backend.user.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class ScheduleService {

    private final CurrentScheduleUserService currentUserService;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleChangeHistoryRepository historyRepository;
    private final ConfirmationService confirmationService;

    public ScheduleService(
            CurrentScheduleUserService currentUserService,
            ScheduleRepository scheduleRepository,
            ScheduleChangeHistoryRepository historyRepository,
            ConfirmationService confirmationService
    ) {
        this.currentUserService = currentUserService;
        this.scheduleRepository = scheduleRepository;
        this.historyRepository = historyRepository;
        this.confirmationService = confirmationService;
    }

    @Transactional(noRollbackFor = ScheduleConflictException.class)
    public ScheduleResponse create(
            Authentication authentication,
            CreateScheduleRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        User user = currentUserService.requireCurrentUser(authentication, request, response);
        Instant startAt = requestBody.startAt().toInstant();
        Instant endAt = requestBody.endAt().toInstant();
        if (!endAt.isAfter(startAt)) {
            throw new InvalidScheduleTimeRangeException();
        }

        List<Schedule> conflicts = scheduleRepository.findConflicts(user, startAt, endAt);
        if (!conflicts.isEmpty()) {
            Long confirmationId = confirmationService.createPending(
                    user,
                    requestBody.title(),
                    startAt,
                    endAt,
                    requestBody.location(),
                    conflicts
            );
            throw new ScheduleConflictException(
                    confirmationId,
                    conflicts.stream().map(ConflictingScheduleResponse::from).toList()
            );
        }

        Schedule schedule = scheduleRepository.saveAndFlush(new Schedule(
                user,
                requestBody.title(),
                requestBody.location(),
                startAt,
                endAt
        ));
        historyRepository.save(ScheduleChangeHistory.created(schedule, user));
        return ScheduleResponse.from(schedule);
    }

    @Transactional(readOnly = true)
    public ScheduleListResponse findInRange(
            Authentication authentication,
            Instant from,
            Instant to,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        User user = currentUserService.requireCurrentUser(authentication, request, response);
        if (!from.isBefore(to)) {
            throw new InvalidScheduleQueryRangeException();
        }

        List<ScheduleListItemResponse> items = scheduleRepository.findInRange(user, from, to)
                .stream()
                .map(ScheduleListItemResponse::from)
                .toList();
        return new ScheduleListResponse(items);
    }

    @Transactional(readOnly = true)
    public ScheduleResponse findById(
            Authentication authentication,
            Long scheduleId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        User user = currentUserService.requireCurrentUser(authentication, request, response);
        return scheduleRepository.findByIdAndOwner(scheduleId, user)
                .map(ScheduleResponse::from)
                .orElseThrow(ScheduleNotFoundException::new);
    }

    @Transactional(noRollbackFor = ScheduleConflictException.class)
    public ScheduleResponse update(
            Authentication authentication,
            Long scheduleId,
            UpdateScheduleRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        User user = currentUserService.requireCurrentUser(authentication, request, response);
        Schedule schedule = scheduleRepository.findByIdAndOwner(scheduleId, user)
                .orElseThrow(ScheduleNotFoundException::new);
        validateUpdate(requestBody);
        if (!schedule.getVersion().equals(requestBody.version())) {
            throw new ScheduleVersionConflictException();
        }

        String title = requestBody.titlePresent() ? requestBody.title() : schedule.getTitle();
        String location = requestBody.locationPresent() ? requestBody.location() : schedule.getLocation();
        Instant startAt = requestBody.startAtPresent()
                ? requestBody.startAt().toInstant() : schedule.getStartAt();
        Instant endAt = requestBody.endAtPresent()
                ? requestBody.endAt().toInstant() : schedule.getEndAt();
        if (!endAt.isAfter(startAt)) {
            throw new InvalidScheduleTimeRangeException();
        }
        if (title.equals(schedule.getTitle())
                && java.util.Objects.equals(location, schedule.getLocation())
                && startAt.equals(schedule.getStartAt())
                && endAt.equals(schedule.getEndAt())) {
            return ScheduleResponse.from(schedule);
        }

        List<Schedule> conflicts = scheduleRepository.findConflictsExcluding(
                user, scheduleId, startAt, endAt);
        if (!conflicts.isEmpty()) {
            Long confirmationId = confirmationService.createPendingUpdate(
                    user, schedule, requestBody, conflicts);
            throw new ScheduleConflictException(
                    confirmationId,
                    conflicts.stream().map(ConflictingScheduleResponse::from).toList()
            );
        }

        String oldTitle = schedule.getTitle();
        String oldLocation = schedule.getLocation();
        Instant oldStartAt = schedule.getStartAt();
        Instant oldEndAt = schedule.getEndAt();
        schedule.update(title, location, startAt, endAt);
        scheduleRepository.saveAndFlush(schedule);
        historyRepository.save(ScheduleChangeHistory.updated(
                schedule, user, oldTitle, oldStartAt, oldEndAt, oldLocation));
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(
            Authentication authentication,
            Long scheduleId,
            Long version,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        User user = currentUserService.requireCurrentUser(authentication, request, response);
        if (version == null || version < 0) {
            throw new InvalidScheduleUpdateException("version", "REQUIRED", "A non-negative version is required.");
        }
        Schedule schedule = scheduleRepository.findByIdAndOwner(scheduleId, user)
                .orElseThrow(ScheduleNotFoundException::new);
        if (!schedule.getVersion().equals(version)) {
            throw new ScheduleVersionConflictException();
        }
        confirmationService.detachPendingUpdates(scheduleId);
        scheduleRepository.delete(schedule);
        scheduleRepository.flush();
    }

    private void validateUpdate(UpdateScheduleRequest body) {
        if (!body.versionPresent() || body.version() == null || body.version() < 0) {
            throw new InvalidScheduleUpdateException("version", "REQUIRED", "A non-negative version is required.");
        }
        if (!body.titlePresent() && !body.startAtPresent()
                && !body.endAtPresent() && !body.locationPresent()) {
            throw new InvalidScheduleUpdateException("request", "REQUIRED", "At least one field must be changed.");
        }
        if (body.titlePresent() && (body.title() == null || body.title().isBlank())) {
            throw new InvalidScheduleUpdateException("title", "REQUIRED", "Title is required.");
        }
        if (body.titlePresent() && body.title().length() > 200) {
            throw new InvalidScheduleUpdateException("title", "MAX_LENGTH", "Title must be at most 200 characters.");
        }
        if (body.locationPresent() && body.location() != null && body.location().length() > 200) {
            throw new InvalidScheduleUpdateException("location", "MAX_LENGTH", "Location must be at most 200 characters.");
        }
        if (body.startAtPresent() && body.startAt() == null) {
            throw new InvalidScheduleUpdateException("startAt", "REQUIRED", "Start time is required.");
        }
        if (body.endAtPresent() && body.endAt() == null) {
            throw new InvalidScheduleUpdateException("endAt", "REQUIRED", "End time is required.");
        }
    }
}
