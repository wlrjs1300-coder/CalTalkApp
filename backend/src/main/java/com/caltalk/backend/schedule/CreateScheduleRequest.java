package com.caltalk.backend.schedule;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateScheduleRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt,
        @Size(max = 200) String location,
        @Size(max = 4) List<Integer> reminderMinutes
) {

    public CreateScheduleRequest {
        title = title == null ? null : title.trim();
        location = location == null || location.trim().isEmpty() ? null : location.trim();
    }

    @JsonAnySetter
    public void rejectUnknownField(String field, Object value) {
        throw new IllegalArgumentException("지원하지 않는 요청 필드입니다.");
    }
}
