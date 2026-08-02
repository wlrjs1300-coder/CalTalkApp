package com.caltalk.backend.user;

import jakarta.validation.constraints.NotBlank;

public record UpdateTimezoneRequest(
        @NotBlank String timezone
) {

    public UpdateTimezoneRequest {
        timezone = timezone == null ? null : timezone.trim();
    }
}
