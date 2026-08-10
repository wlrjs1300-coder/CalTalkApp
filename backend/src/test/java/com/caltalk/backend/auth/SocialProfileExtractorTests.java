package com.caltalk.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

class SocialProfileExtractorTests {

    @Test
    void extractsAndNormalizesGoogleProfile() {
        SocialProfile profile = SocialProfileExtractor.extract("google", Map.of(
                "sub", "google-user-1",
                "email", "Member@Example.com",
                "email_verified", true
        ));

        assertThat(profile).isEqualTo(new SocialProfile(
                "google", "google-user-1", "member@example.com"
        ));
    }

    @Test
    void extractsKakaoAndNaverNestedProfiles() {
        SocialProfile kakao = SocialProfileExtractor.extract("kakao", Map.of(
                "id", 1234L,
                "kakao_account", Map.of("email", "kakao@example.com")
        ));
        SocialProfile naver = SocialProfileExtractor.extract("naver", Map.of(
                "response", Map.of("id", "naver-user-1", "email", "naver@example.com")
        ));

        assertThat(kakao.subject()).isEqualTo("1234");
        assertThat(naver.email()).isEqualTo("naver@example.com");
    }

    @Test
    void rejectsUnverifiedOrMissingEmail() {
        assertThatThrownBy(() -> SocialProfileExtractor.extract("google", Map.of(
                "sub", "google-user-1",
                "email", "member@example.com",
                "email_verified", false
        ))).isInstanceOf(OAuth2AuthenticationException.class);

        assertThatThrownBy(() -> SocialProfileExtractor.extract("kakao", Map.of(
                "id", 1234L,
                "kakao_account", Map.of(
                        "email", "kakao@example.com",
                        "is_email_verified", false
                )
        ))).isInstanceOf(OAuth2AuthenticationException.class);
    }
}
