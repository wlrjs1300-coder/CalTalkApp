package com.caltalk.backend.confirmation;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.caltalk.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "confirmation_requests")
public class ConfirmationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "origin_channel", nullable = false, length = 20)
    private String originChannel;

    @Column(name = "command_type", nullable = false, length = 30)
    private String commandType;

    @Column(name = "target_schedule_id")
    private Long targetScheduleId;

    @Column(name = "target_schedule_version")
    private Long targetScheduleVersion;

    @Column(length = 200)
    private String title;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "location_action", nullable = false, length = 10)
    private String locationAction;

    @Column(name = "location_value", length = 200)
    private String locationValue;

    @Column(name = "candidate_fingerprint", nullable = false, columnDefinition = "CHAR(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String candidateFingerprint;

    @Column(name = "conflict_snapshot_hash", nullable = false, columnDefinition = "CHAR(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String conflictSnapshotHash;

    @Column(name = "conflict_acknowledged", nullable = false)
    private boolean conflictAcknowledged;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConfirmationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "superseded_by_confirmation_id")
    private ConfirmationRequest supersededBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected ConfirmationRequest() {
    }

    public ConfirmationRequest(
            User user,
            String title,
            Instant startAt,
            Instant endAt,
            String location,
            String candidateFingerprint,
            String conflictSnapshotHash,
            Instant createdAt
    ) {
        this.user = user;
        this.originChannel = "PWA";
        this.commandType = "CREATE_EVENT";
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
        this.locationAction = location == null ? "REMOVE" : "SET";
        this.locationValue = location;
        this.candidateFingerprint = candidateFingerprint;
        this.conflictSnapshotHash = conflictSnapshotHash;
        this.status = ConfirmationStatus.PENDING;
        this.createdAt = createdAt;
        this.expiresAt = createdAt.plusSeconds(300);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public String getLocationValue() {
        return locationValue;
    }

    public String getCandidateFingerprint() {
        return candidateFingerprint;
    }

    public String getConflictSnapshotHash() {
        return conflictSnapshotHash;
    }

    public ConfirmationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void expire() {
        status = ConfirmationStatus.EXPIRED;
    }

    public void consume(Instant now) {
        conflictAcknowledged = true;
        status = ConfirmationStatus.CONSUMED;
        consumedAt = now;
    }

    public void supersedeBy(ConfirmationRequest replacement) {
        status = ConfirmationStatus.SUPERSEDED;
        supersededBy = replacement;
    }
}
