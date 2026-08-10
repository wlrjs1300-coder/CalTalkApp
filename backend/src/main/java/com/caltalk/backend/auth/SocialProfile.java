package com.caltalk.backend.auth;

public record SocialProfile(String provider, String subject, String email) {
}
