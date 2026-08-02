package com.caltalk.backend.user;

import java.time.Instant;

public record CurrentUserResponse(
        String email,
        String timezone,
        Instant createdAt
) {
}
