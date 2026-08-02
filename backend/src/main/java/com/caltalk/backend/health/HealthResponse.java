package com.caltalk.backend.health;

import java.time.Instant;

public record HealthResponse(
        String status,
        String service,
        Instant timestamp
) {
}
