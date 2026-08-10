package com.caltalk.backend.notification;

import java.time.Instant;

import com.caltalk.backend.schedule.Schedule;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Entity
@Table(name = "reminder_deliveries")
class ReminderDelivery {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;
    @Column(name = "reminder_minutes", nullable = false)
    private int reminderMinutes;
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
    protected ReminderDelivery() {}
    ReminderDelivery(Schedule schedule, int reminderMinutes, Instant sentAt) {
        this.schedule = schedule; this.reminderMinutes = reminderMinutes; this.sentAt = sentAt;
    }
}
