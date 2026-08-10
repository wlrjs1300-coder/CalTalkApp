package com.caltalk.backend.auth;

import java.util.Locale;
import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public final class SocialProfileExtractor {

    private SocialProfileExtractor() {
    }

    public static SocialProfile extract(String provider, Map<String, Object> attributes) {
        return switch (provider) {
            case "google" -> google(attributes);
            case "kakao" -> kakao(attributes);
            case "naver" -> naver(attributes);
            default -> fail("unsupported_provider");
        };
    }

    private static SocialProfile google(Map<String, Object> attributes) {
        if (!Boolean.TRUE.equals(attributes.get("email_verified"))) return fail("email_not_verified");
        return profile("google", attributes.get("sub"), attributes.get("email"));
    }

    private static SocialProfile kakao(Map<String, Object> attributes) {
        Map<String, Object> account = child(attributes, "kakao_account");
        if (Boolean.TRUE.equals(account.get("email_needs_agreement"))
                || Boolean.FALSE.equals(account.get("is_email_valid"))
                || Boolean.FALSE.equals(account.get("is_email_verified"))) {
            return fail("email_not_verified");
        }
        return profile("kakao", attributes.get("id"), account.get("email"));
    }

    private static SocialProfile naver(Map<String, Object> attributes) {
        Map<String, Object> response = child(attributes, "response");
        return profile("naver", response.get("id"), response.get("email"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> child(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
        return fail("profile_missing");
    }

    private static SocialProfile profile(String provider, Object subjectValue, Object emailValue) {
        String subject = subjectValue == null ? "" : subjectValue.toString().trim();
        String email = emailValue == null
                ? ""
                : emailValue.toString().trim().toLowerCase(Locale.ROOT);
        if (subject.isBlank() || email.isBlank() || !email.contains("@")) {
            return fail("profile_missing");
        }
        return new SocialProfile(provider, subject, email);
    }

    private static <T> T fail(String code) {
        throw new OAuth2AuthenticationException(new OAuth2Error(code));
    }
}
