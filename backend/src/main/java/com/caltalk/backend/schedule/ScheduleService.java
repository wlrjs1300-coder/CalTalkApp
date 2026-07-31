package com.caltalk.backend.schedule;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.common.error.InvalidScheduleTimeRangeException;
import com.caltalk.backend.common.error.InvalidScheduleQueryRangeException;
import com.caltalk.backend.common.error.ScheduleConflictException;
import com.caltalk.backend.common.error.ScheduleNotFoundException;
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
}
