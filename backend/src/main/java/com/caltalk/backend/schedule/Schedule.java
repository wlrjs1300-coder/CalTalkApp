package com.caltalk.backend.schedule;

import java.time.Instant;
import java.util.List;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "schedules")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String location;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "reminder_minutes", nullable = false, length = 100)
    private String reminderMinutes = "1440";

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Schedule() {
    }

    public Schedule(User owner, String title, String location, Instant startAt, Instant endAt) {
        this.owner = owner;
        this.title = title;
        this.location = location;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void update(String title, String location, Instant startAt, Instant endAt) {
        this.title = title;
        this.location = location;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void changeReminderMinutes(List<Integer> values) {
        reminderMinutes = values.stream().distinct().sorted(java.util.Comparator.reverseOrder())
                .map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    public List<Integer> getReminderMinutes() {
        if (reminderMinutes == null || reminderMinutes.isBlank()) return List.of();
        return java.util.Arrays.stream(reminderMinutes.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).map(Integer::valueOf).toList();
    }

    @PrePersist
    void assignTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }
    public User getOwner() { return owner; }

    public String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
