package com.caltalk.backend.confirmation;

import jakarta.validation.constraints.AssertTrue;

public record ApproveConfirmationRequest(
        @AssertTrue boolean conflictAcknowledged
) {
}
