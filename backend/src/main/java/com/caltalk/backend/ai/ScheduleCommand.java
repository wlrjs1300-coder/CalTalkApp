package com.caltalk.backend.ai;

import java.util.List;

public record ScheduleCommand(
        String schemaVersion,
        ScheduleIntent intent,
        CommandStatus status,
        String title,
        String dateExpression,
        String startTime,
        String endTime,
        String targetExpression,
        List<String> missingFields,
        List<String> ambiguities,
        String userFacingQuestion
) {
    public ScheduleCommand {
        missingFields = missingFields == null ? List.of() : List.copyOf(missingFields);
        ambiguities = ambiguities == null ? List.of() : List.copyOf(ambiguities);
    }
}
