package com.caltalk.backend.auth;

public record LoginResponse(
        String email,
        String timezone
) {
}
