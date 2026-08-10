package com.caltalk.backend.notification;

import java.time.Instant;
import java.time.LocalDate;

import com.caltalk.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "summary_deliveries", uniqueConstraints = @UniqueConstraint(
        name = "uq_summary_delivery", columnNames = {"user_id", "summary_type", "period_start"}))
class SummaryDelivery {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "summary_type", nullable = false, length = 10)
    private String type;
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
    protected SummaryDelivery() {}
    SummaryDelivery(User user, String type, LocalDate periodStart, Instant sentAt) {
        this.user = user; this.type = type; this.periodStart = periodStart; this.sentAt = sentAt;
    }
}
