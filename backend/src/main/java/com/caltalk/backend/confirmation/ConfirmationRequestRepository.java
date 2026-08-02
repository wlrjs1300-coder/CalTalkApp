package com.caltalk.backend.confirmation;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.caltalk.backend.user.User;

public interface ConfirmationRequestRepository
        extends JpaRepository<ConfirmationRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select confirmation from ConfirmationRequest confirmation where confirmation.id = :id")
    Optional<ConfirmationRequest> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select confirmation from ConfirmationRequest confirmation
            where confirmation.user = :user
              and confirmation.candidateFingerprint = :fingerprint
              and confirmation.status = com.caltalk.backend.confirmation.ConfirmationStatus.PENDING
            """)
    Optional<ConfirmationRequest> findPendingCandidateForUpdate(
            @Param("user") User user,
            @Param("fingerprint") String fingerprint
    );

    List<ConfirmationRequest> findByTargetScheduleId(Long targetScheduleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ConfirmationRequest confirmation
            set confirmation.supersededBy = null
            where confirmation.user = :user
            """)
    void clearSupersededReferencesByUser(@Param("user") User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ConfirmationRequest confirmation where confirmation.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
