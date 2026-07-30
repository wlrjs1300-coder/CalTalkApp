package com.caltalk.backend.auth;

import java.time.Instant;

public record SignupResponse(
        String email,
        String timezone,
        Instant createdAt
) {
}
