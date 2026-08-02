package com.caltalk.backend.schedule.history;

import java.time.Instant;

import com.caltalk.backend.schedule.Schedule;
import com.caltalk.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "schedule_change_history")
public class ScheduleChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedBy;

    @Column(name = "source_channel", nullable = false, length = 20)
    private String sourceChannel;

    @Column(name = "change_type", nullable = false, length = 20)
    private String changeType;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Column(name = "title_before", length = 200)
    private String titleBefore;

    @Column(name = "title_after", length = 200)
    private String titleAfter;

    @Column(name = "start_at_before")
    private Instant startAtBefore;

    @Column(name = "start_at_after")
    private Instant startAtAfter;

    @Column(name = "end_at_before")
    private Instant endAtBefore;

    @Column(name = "end_at_after")
    private Instant endAtAfter;

    @Column(name = "location_before", length = 200)
    private String locationBefore;

    @Column(name = "location_after", length = 200)
    private String locationAfter;

    protected ScheduleChangeHistory() {
    }

    public static ScheduleChangeHistory created(Schedule schedule, User user) {
        ScheduleChangeHistory history = new ScheduleChangeHistory();
        history.schedule = schedule;
        history.changedBy = user;
        history.sourceChannel = "PWA";
        history.changeType = "CREATE";
        history.titleAfter = schedule.getTitle();
        history.startAtAfter = schedule.getStartAt();
        history.endAtAfter = schedule.getEndAt();
        history.locationAfter = schedule.getLocation();
        return history;
    }

    public static ScheduleChangeHistory updated(
            Schedule schedule,
            User user,
            String titleBefore,
            Instant startAtBefore,
            Instant endAtBefore,
            String locationBefore
    ) {
        ScheduleChangeHistory history = new ScheduleChangeHistory();
        history.schedule = schedule;
        history.changedBy = user;
        history.sourceChannel = "PWA";
        history.changeType = "UPDATE";
        history.titleBefore = titleBefore;
        history.startAtBefore = startAtBefore;
        history.endAtBefore = endAtBefore;
        history.locationBefore = locationBefore;
        history.titleAfter = schedule.getTitle();
        history.startAtAfter = schedule.getStartAt();
        history.endAtAfter = schedule.getEndAt();
        history.locationAfter = schedule.getLocation();
        return history;
    }

    @PrePersist
    void assignChangedAt() {
        changedAt = Instant.now();
    }
}
