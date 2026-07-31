package com.caltalk.backend.schedule;

import java.time.Instant;

public record ConflictingScheduleResponse(
        Long id,
        String title,
        Instant startAt,
        Instant endAt,
        String location
) {

    public static ConflictingScheduleResponse from(Schedule schedule) {
        return new ConflictingScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getLocation()
        );
    }
}
