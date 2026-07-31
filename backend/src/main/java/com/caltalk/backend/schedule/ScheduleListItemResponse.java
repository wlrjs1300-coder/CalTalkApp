package com.caltalk.backend.schedule;

import java.time.Instant;

public record ScheduleListItemResponse(
        Long id,
        String title,
        Instant startAt,
        Instant endAt,
        String location,
        Long version
) {

    public static ScheduleListItemResponse from(Schedule schedule) {
        return new ScheduleListItemResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getLocation(),
                schedule.getVersion()
        );
    }
}
