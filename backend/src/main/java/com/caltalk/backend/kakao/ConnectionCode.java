package com.caltalk.backend.kakao;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "connection_codes")
public class ConnectionCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "code_hmac", nullable = false, length = 64, unique = true)
    private String codeHmac;
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "used_at")
    private Instant usedAt;
    @Column(name = "invalidated_at")
    private Instant invalidatedAt;
    @Column(name = "fail_count", nullable = false)
    private int failCount;

    protected ConnectionCode() {}
    public ConnectionCode(User user, String codeHmac, Instant issuedAt, Instant expiresAt) {
        this.user=user; this.codeHmac=codeHmac; this.issuedAt=issuedAt; this.expiresAt=expiresAt;
    }
    public User getUser(){ return user; }
    public Instant getExpiresAt(){ return expiresAt; }
    public boolean isUsable(Instant now){ return usedAt==null && invalidatedAt==null && failCount<5 && expiresAt.isAfter(now); }
    public void use(Instant now){ usedAt=now; }
    public void invalidate(Instant now){ invalidatedAt=now; }
}
