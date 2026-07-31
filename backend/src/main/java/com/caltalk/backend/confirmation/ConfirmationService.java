package com.caltalk.backend.confirmation;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.common.error.ConfirmationForbiddenException;
import com.caltalk.backend.common.error.ConfirmationNotFoundException;
import com.caltalk.backend.common.error.ConfirmationSupersededException;
import com.caltalk.backend.common.error.ConflictAcknowledgementRequiredException;
import com.caltalk.backend.schedule.ConflictingScheduleResponse;
import com.caltalk.backend.schedule.CurrentScheduleUserService;
import com.caltalk.backend.schedule.Schedule;
import com.caltalk.backend.schedule.ScheduleRepository;
import com.caltalk.backend.schedule.ScheduleResponse;
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

    public ConfirmationService(
            ConfirmationRequestRepository confirmationRepository,
            ConfirmationFingerprintService fingerprintService,
            CurrentScheduleUserService currentUserService,
            ScheduleRepository scheduleRepository,
            ScheduleChangeHistoryRepository historyRepository
    ) {
        this.confirmationRepository = confirmationRepository;
        this.fingerprintService = fingerprintService;
        this.currentUserService = currentUserService;
        this.scheduleRepository = scheduleRepository;
        this.historyRepository = historyRepository;
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

    @Transactional(noRollbackFor = {
            ConfirmationNotFoundException.class,
            ConfirmationSupersededException.class
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
}
