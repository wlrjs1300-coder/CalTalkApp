package com.caltalk.backend.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocialIdentityRepository extends JpaRepository<SocialIdentity, Long> {
    Optional<SocialIdentity> findByProviderAndProviderSubject(String provider, String providerSubject);

    @Query("""
            select user.email
            from SocialIdentity identity
            join identity.user user
            where identity.provider = :provider
              and identity.providerSubject = :providerSubject
            """)
    Optional<String> findUserEmailByProviderAndProviderSubject(
            @Param("provider") String provider,
            @Param("providerSubject") String providerSubject
    );
}
