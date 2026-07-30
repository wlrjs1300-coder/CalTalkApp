package com.caltalk.backend.common.error;

public record FieldErrorResponse(
        String field,
        String code,
        String message
) {
}
