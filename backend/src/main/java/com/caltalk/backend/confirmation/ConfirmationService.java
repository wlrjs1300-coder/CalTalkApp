package com.caltalk.backend.confirmation;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.common.error.ConfirmationForbiddenException;
import com.caltalk.backend.common.error.ConfirmationNotFoundException;
import com.caltalk.backend.common.error.ConfirmationSupersededException;
import com.caltalk.backend.common.error.ConfirmationTargetGoneException;
import com.caltalk.backend.common.error.ConflictAcknowledgementRequiredException;
import com.caltalk.backend.schedule.ConflictingScheduleResponse;
import com.caltalk.backend.schedule.CurrentScheduleUserService;
import com.caltalk.backend.schedule.Schedule;
import com.caltalk.backend.schedule.ScheduleRepository;
import com.caltalk.backend.schedule.ScheduleResponse;
import com.caltalk.backend.schedule.UpdateScheduleRequest;
import com.caltalk.backend.schedule.history.ScheduleChangeHistory;
import com.caltalk.backend.schedule.history.ScheduleChangeHistoryRepository;
import com.caltalk.backend.user.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class ConfirmationService {

    private final ConfirmationRequestRepository confirmationRepository;
    private final ConfirmationFingerprintService fingerprintService;
    private final CurrentScheduleUserService currentUserService;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleChangeHistoryRepository historyRepository;
    private final PendingConfirmationInsert pendingInsert;

    public ConfirmationService(
            ConfirmationRequestRepository confirmationRepository,
            ConfirmationFingerprintService fingerprintService,
            CurrentScheduleUserService currentUserService,
            ScheduleRepository scheduleRepository,
            ScheduleChangeHistoryRepository historyRepository,
            PendingConfirmationInsert pendingInsert
    ) {
        this.confirmationRepository = confirmationRepository;
        this.fingerprintService = fingerprintService;
        this.currentUserService = currentUserService;
        this.scheduleRepository = scheduleRepository;
        this.historyRepository = historyRepository;
        this.pendingInsert = pendingInsert;
    }

    public Long createPending(
            User user,
            String title,
            Instant startAt,
            Instant endAt,
            String location,
            List<Schedule> conflicts
    ) {
        String candidateFingerprint = fingerprintService.candidate(
                title,
                startAt,
                endAt,
                location
        );
        String conflictHash = fingerprintService.conflicts(conflicts);
        ConfirmationRequest existing = confirmationRepository.findPendingCandidateForUpdate(
                user,
                candidateFingerprint
        ).orElse(null);
        if (existing != null
                && existing.getExpiresAt().isAfter(Instant.now())
                && existing.getConflictSnapshotHash().equals(conflictHash)) {
            return existing.getId();
        }
        if (existing != null) {
            existing.expire();
            confirmationRepository.flush();
        }

        if (existing == null) {
            Instant now = Instant.now();
            Long insertedId = pendingInsert.createEvent(
                    user.getEmail(), title, startAt, endAt, location,
                    candidateFingerprint, conflictHash, now);
            if (insertedId != null) {
                return insertedId;
            }
            return convergeConcurrentCreate(
                    user, title, startAt, endAt, location, candidateFingerprint);
        }

        ConfirmationRequest confirmation = new ConfirmationRequest(
                user,
                title,
                startAt,
                endAt,
                location,
                candidateFingerprint,
                conflictHash,
                Instant.now()
        );
        Long confirmationId = confirmationRepository.saveAndFlush(confirmation).getId();
        if (existing != null) {
            existing.supersedeBy(confirmation);
        }
        return confirmationId;
    }

    public Long createPendingUpdate(
            User user,
            Schedule target,
            UpdateScheduleRequest update,
            List<Schedule> conflicts
    ) {
        String title = update.titlePresent() ? update.title() : null;
        Instant startAt = update.startAtPresent() ? update.startAt().toInstant() : null;
        Instant endAt = update.endAtPresent() ? update.endAt().toInstant() : null;
        String locationAction = update.locationPresent()
                ? (update.location() == null ? "REMOVE" : "SET") : "KEEP";
        String locationValue = "SET".equals(locationAction) ? update.location() : null;
        return createPendingUpdate(
                user, target.getId(), target.getVersion(), title, startAt, endAt,
                locationAction, locationValue, conflicts);
    }

    private Long createPendingUpdate(
            User user,
            Long targetId,
            Long targetVersion,
            String title,
            Instant startAt,
            Instant endAt,
            String locationAction,
            String locationValue,
            List<Schedule> conflicts
    ) {
        String fingerprint = fingerprintService.updateCandidate(
                targetId, title, startAt, endAt, locationAction, locationValue);
        String conflictHash = fingerprintService.conflicts(conflicts);
        ConfirmationRequest existing = confirmationRepository
                .findPendingCandidateForUpdate(user, fingerprint).orElse(null);
        if (existing != null
                && existing.getExpiresAt().isAfter(Instant.now())
                && existing.getTargetScheduleVersion().equals(targetVersion)
                && existing.getConflictSnapshotHash().equals(conflictHash)) {
            return existing.getId();
        }
        if (existing != null) {
            existing.expire();
            confirmationRepository.flush();
        }
        if (existing == null) {
            Instant now = Instant.now();
            Long insertedId = pendingInsert.updateEvent(
                    user.getEmail(), targetId, targetVersion, title, startAt, endAt,
                    locationAction, locationValue, fingerprint, conflictHash, now);
            if (insertedId != null) {
                return insertedId;
            }
            return convergeConcurrentUpdate(
                    user, targetId, title, startAt, endAt,
                    locationAction, locationValue, fingerprint);
        }
        ConfirmationRequest confirmation = ConfirmationRequest.update(
                user, targetId, targetVersion, title, startAt, endAt,
                locationAction, locationValue, fingerprint, conflictHash, Instant.now());
        Long id = confirmationRepository.saveAndFlush(confirmation).getId();
        if (existing != null) {
            existing.supersedeBy(confirmation);
        }
        return id;
    }

    private Long convergeConcurrentCreate(
            User user,
            String title,
            Instant startAt,
            Instant endAt,
            String location,
            String fingerprint
    ) {
        for (int attempt = 0; attempt < 3; attempt++) {
            String latestConflictHash = fingerprintService.conflicts(
                    scheduleRepository.findConflicts(user, startAt, endAt));
            ConfirmationRequest pending = confirmationRepository
                    .findPendingCandidateForUpdate(user, fingerprint).orElse(null);
            if (pending == null) {
                Instant now = Instant.now();
                Long insertedId = pendingInsert.createEvent(
                        user.getEmail(), title, startAt, endAt, location,
                        fingerprint, latestConflictHash, now);
                if (insertedId != null) {
                    return insertedId;
                }
                continue;
            }
            if (pending.getExpiresAt().isAfter(Instant.now())
                    && pending.getConflictSnapshotHash().equals(latestConflictHash)) {
                return pending.getId();
            }
            pending.expire();
            confirmationRepository.flush();
            ConfirmationRequest replacement = confirmationRepository.saveAndFlush(
                    new ConfirmationRequest(
                            user, title, startAt, endAt, location,
                            fingerprint, latestConflictHash, Instant.now()));
            pending.supersedeBy(replacement);
            return replacement.getId();
        }
        throw new IllegalStateException("Concurrent confirmation did not converge after 3 attempts.");
    }

    private Long convergeConcurrentUpdate(
            User user,
            Long targetId,
            String title,
            Instant startAt,
            Instant endAt,
            String locationAction,
            String locationValue,
            String fingerprint
    ) {
        for (int attempt = 0; attempt < 3; attempt++) {
            Schedule latestTarget = scheduleRepository.findByIdAndOwner(targetId, user)
                    .orElseThrow(ConfirmationTargetGoneException::new);
            Long latestTargetVersion = latestTarget.getVersion();
            Instant candidateStart = startAt == null ? latestTarget.getStartAt() : startAt;
            Instant candidateEnd = endAt == null ? latestTarget.getEndAt() : endAt;
            String latestConflictHash = fingerprintService.conflicts(
                    scheduleRepository.findConflictsExcluding(
                            user, targetId, candidateStart, candidateEnd));
            ConfirmationRequest pending = confirmationRepository
                    .findPendingCandidateForUpdate(user, fingerprint).orElse(null);
            if (pending == null) {
                Instant now = Instant.now();
                Long insertedId = pendingInsert.updateEvent(
                        user.getEmail(), targetId, latestTargetVersion, title, startAt, endAt,
                        locationAction, locationValue, fingerprint, latestConflictHash, now);
                if (insertedId != null) {
                    return insertedId;
                }
                continue;
            }
            if (pending.getExpiresAt().isAfter(Instant.now())
                    && latestTargetVersion.equals(pending.getTargetScheduleVersion())
                    && pending.getConflictSnapshotHash().equals(latestConflictHash)) {
                return pending.getId();
            }
            pending.expire();
            confirmationRepository.flush();
            ConfirmationRequest replacement = confirmationRepository.saveAndFlush(
                    ConfirmationRequest.update(
                            user, targetId, latestTargetVersion, title, startAt, endAt,
                            locationAction, locationValue, fingerprint,
                            latestConflictHash, Instant.now()));
            pending.supersedeBy(replacement);
            return replacement.getId();
        }
        throw new IllegalStateException("Concurrent confirmation did not converge after 3 attempts.");
    }

    public void detachPendingUpdates(Long scheduleId) {
        confirmationRepository.findByTargetScheduleId(scheduleId)
                .forEach(ConfirmationRequest::detachTarget);
        confirmationRepository.flush();
    }

    @Transactional(noRollbackFor = {
            ConfirmationNotFoundException.class,
            ConfirmationSupersededException.class,
            ConfirmationTargetGoneException.class
    })
    public ScheduleResponse approve(
            Long confirmationId,
            ApproveConfirmationRequest requestBody,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        User user = currentUserService.requireCurrentUser(authentication, request, response);
        ConfirmationRequest confirmation = confirmationRepository.findByIdForUpdate(confirmationId)
                .orElseThrow(ConfirmationNotFoundException::new);

        if (!confirmation.getUser().getEmail().equals(user.getEmail())) {
            throw new ConfirmationForbiddenException();
        }
        if (confirmation.getStatus() != ConfirmationStatus.PENDING) {
            throw new ConfirmationNotFoundException();
        }
        if (!confirmation.getExpiresAt().isAfter(Instant.now())) {
            confirmation.expire();
            throw new ConfirmationNotFoundException();
        }

        if ("UPDATE_EVENT".equals(confirmation.getCommandType())) {
            return approveUpdate(confirmation, requestBody, user);
        }

        String candidateFingerprint = fingerprintService.candidate(
                confirmation.getTitle(),
                confirmation.getStartAt(),
                confirmation.getEndAt(),
                confirmation.getLocationValue()
        );
        if (!candidateFingerprint.equals(confirmation.getCandidateFingerprint())) {
            throw new ConfirmationNotFoundException();
        }

        List<Schedule> conflicts = scheduleRepository.findConflicts(
                user,
                confirmation.getStartAt(),
                confirmation.getEndAt()
        );
        String conflictHash = fingerprintService.conflicts(conflicts);
        if (!conflictHash.equals(confirmation.getConflictSnapshotHash())) {
            confirmation.expire();
            confirmationRepository.flush();
            Long replacementId = createPending(
                    user,
                    confirmation.getTitle(),
                    confirmation.getStartAt(),
                    confirmation.getEndAt(),
                    confirmation.getLocationValue(),
                    conflicts
            );
            ConfirmationRequest replacement = confirmationRepository.getReferenceById(replacementId);
            confirmation.supersedeBy(replacement);
            throw new ConfirmationSupersededException(
                    replacementId,
                    conflicts.stream().map(ConflictingScheduleResponse::from).toList()
            );
        }
        if (!requestBody.conflictAcknowledged()) {
            throw new ConflictAcknowledgementRequiredException();
        }

        Schedule schedule = scheduleRepository.saveAndFlush(new Schedule(
                user,
                confirmation.getTitle(),
                confirmation.getLocationValue(),
                confirmation.getStartAt(),
                confirmation.getEndAt()
        ));
        historyRepository.save(ScheduleChangeHistory.created(schedule, user));
        confirmation.consume(Instant.now());
        return ScheduleResponse.from(schedule);
    }

    private ScheduleResponse approveUpdate(
            ConfirmationRequest confirmation,
            ApproveConfirmationRequest requestBody,
            User user
    ) {
        if (confirmation.getTargetScheduleId() == null) {
            confirmation.supersedeBy(null);
            throw new ConfirmationTargetGoneException();
        }
        Schedule target = scheduleRepository.findByIdAndOwner(
                confirmation.getTargetScheduleId(), user).orElse(null);
        if (target == null) {
            confirmation.supersedeBy(null);
            throw new ConfirmationTargetGoneException();
        }
        String fingerprint = fingerprintService.updateCandidate(
                target.getId(),
                confirmation.getTitle(),
                confirmation.getStartAt(),
                confirmation.getEndAt(),
                confirmation.getLocationAction(),
                confirmation.getLocationValue()
        );
        if (!fingerprint.equals(confirmation.getCandidateFingerprint())) {
            throw new ConfirmationNotFoundException();
        }

        String candidateTitle = confirmation.getTitle() == null
                ? target.getTitle() : confirmation.getTitle();
        Instant candidateStart = confirmation.getStartAt() == null
                ? target.getStartAt() : confirmation.getStartAt();
        Instant candidateEnd = confirmation.getEndAt() == null
                ? target.getEndAt() : confirmation.getEndAt();
        String candidateLocation = switch (confirmation.getLocationAction()) {
            case "KEEP" -> target.getLocation();
            case "REMOVE" -> null;
            case "SET" -> confirmation.getLocationValue();
            default -> throw new ConfirmationNotFoundException();
        };
        List<Schedule> conflicts = scheduleRepository.findConflictsExcluding(
                user, target.getId(), candidateStart, candidateEnd);
        String conflictHash = fingerprintService.conflicts(conflicts);
        if (!target.getVersion().equals(confirmation.getTargetScheduleVersion())
                || !conflictHash.equals(confirmation.getConflictSnapshotHash())) {
            confirmation.expire();
            confirmationRepository.flush();
            Long replacementId = createPendingUpdate(
                    user, target.getId(), target.getVersion(),
                    confirmation.getTitle(), confirmation.getStartAt(), confirmation.getEndAt(),
                    confirmation.getLocationAction(), confirmation.getLocationValue(), conflicts);
            ConfirmationRequest replacement = confirmationRepository.getReferenceById(replacementId);
            confirmation.supersedeBy(replacement);
            throw new ConfirmationSupersededException(
                    replacementId,
                    conflicts.stream().map(ConflictingScheduleResponse::from).toList());
        }
        if (!requestBody.conflictAcknowledged()) {
            throw new ConflictAcknowledgementRequiredException();
        }

        String oldTitle = target.getTitle();
        String oldLocation = target.getLocation();
        Instant oldStart = target.getStartAt();
        Instant oldEnd = target.getEndAt();
        target.update(candidateTitle, candidateLocation, candidateStart, candidateEnd);
        scheduleRepository.saveAndFlush(target);
        historyRepository.save(ScheduleChangeHistory.updated(
                target, user, oldTitle, oldStart, oldEnd, oldLocation));
        confirmation.consume(Instant.now());
        return ScheduleResponse.from(target);
    }
}
