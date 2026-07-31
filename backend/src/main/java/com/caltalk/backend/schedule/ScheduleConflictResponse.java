package com.caltalk.backend.schedule;

import java.time.Instant;
import java.util.List;

import com.caltalk.backend.common.error.FieldErrorResponse;

public record ScheduleConflictResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        List<FieldErrorResponse> fieldErrors,
        Long confirmationId,
        List<ConflictingScheduleResponse> conflicts
) {
}
