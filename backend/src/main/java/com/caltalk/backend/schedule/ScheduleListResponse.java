package com.caltalk.backend.schedule;

import java.util.List;

public record ScheduleListResponse(
        List<ScheduleListItemResponse> items
) {

    public ScheduleListResponse {
        items = List.copyOf(items);
    }
}
