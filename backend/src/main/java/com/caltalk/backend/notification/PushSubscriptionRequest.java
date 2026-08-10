package com.caltalk.backend.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record PushSubscriptionRequest(@NotBlank String endpoint, @NotNull @Valid Keys keys) {
    record Keys(@NotBlank String p256dh, @NotBlank String auth) {}
}
