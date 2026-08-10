package com.caltalk.backend.notification;

import java.time.Instant;

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

@Entity
@Table(name = "push_subscriptions")
public class PushSubscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String endpoint;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String p256dh;
    @Column(name = "auth_secret", nullable = false, columnDefinition = "TEXT")
    private String authSecret;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PushSubscription() {}
    PushSubscription(User user, String endpoint, String p256dh, String authSecret) {
        this.user = user; this.endpoint = endpoint; this.p256dh = p256dh; this.authSecret = authSecret;
    }
    void refresh(User user, String p256dh, String authSecret) {
        this.user = user; this.p256dh = p256dh; this.authSecret = authSecret;
    }
    @PrePersist void createTimestamps() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void updateTimestamp() { updatedAt = Instant.now(); }
    public String getEndpoint() { return endpoint; }
    public String getP256dh() { return p256dh; }
    public String getAuthSecret() { return authSecret; }
}
