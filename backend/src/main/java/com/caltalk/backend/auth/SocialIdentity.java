package com.caltalk.backend.auth;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "social_identities", uniqueConstraints = {
        @UniqueConstraint(name = "uq_social_identities_provider_subject", columnNames = {
                "provider", "provider_subject"
        }),
        @UniqueConstraint(name = "uq_social_identities_user_provider", columnNames = {
                "user_id", "provider"
        })
})
public class SocialIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(name = "provider_email", nullable = false, length = 254)
    private String providerEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SocialIdentity() {
    }

    public SocialIdentity(User user, String provider, String providerSubject, String providerEmail) {
        this.user = user;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.providerEmail = providerEmail;
    }

    @PrePersist
    void assignCreatedAt() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public User getUser() {
        return user;
    }
}
