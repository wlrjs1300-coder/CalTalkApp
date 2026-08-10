package com.caltalk.backend.kakao;

import java.time.Instant;
import com.caltalk.backend.user.User;
import jakarta.persistence.*;

@Entity
@Table(name="kakao_user_links")
public class KakaoUserLink {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false, unique=true) private User user;
    @Column(name="logical_bot_key", nullable=false, length=100) private String logicalBotKey;
    @Column(name="external_user_hmac", nullable=false, length=64) private String externalUserHmac;
    @Column(name="linked_at", nullable=false) private Instant linkedAt;
    protected KakaoUserLink() {}
    public KakaoUserLink(User user,String logicalBotKey,String externalUserHmac,Instant linkedAt){this.user=user;this.logicalBotKey=logicalBotKey;this.externalUserHmac=externalUserHmac;this.linkedAt=linkedAt;}
    public User getUser(){return user;}
    public Instant getLinkedAt(){return linkedAt;}
}
