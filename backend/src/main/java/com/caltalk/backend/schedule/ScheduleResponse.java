package com.caltalk.backend.schedule;

import java.time.Instant;
import java.util.List;

public record ScheduleResponse(
        Long id,
        String title,
        Instant startAt,
        Instant endAt,
        String location,
        List<Integer> reminderMinutes,
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
                schedule.getReminderMinutes(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt(),
                schedule.getVersion()
        );
    }
}
