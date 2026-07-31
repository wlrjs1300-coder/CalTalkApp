package com.caltalk.backend.schedule;

import java.time.Instant;

public record ScheduleResponse(
        Long id,
        String title,
        Instant startAt,
        Instant endAt,
        String location,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {

    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getLocation(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt(),
                schedule.getVersion()
        );
    }
}
