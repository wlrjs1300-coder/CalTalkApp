package com.caltalk.backend.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateChatPreferencesRequest(
        @NotBlank String replyStyle,
        @NotBlank String replyDensity,
        @NotBlank String replyLayout,
        @NotBlank String emojiLevel,
        @NotBlank String timeFormat,
        boolean confirmCreate,
        boolean confirmUpdate,
        @Min(30) @Max(120) int defaultDurationMinutes,
        @NotNull @Size(max = 4) List<Integer> defaultReminderMinutes,
        @NotBlank String defaultQueryRange,
        Boolean dailySummaryEnabled,
        String dailySummaryTime,
        Boolean weeklySummaryEnabled,
        Integer weeklySummaryDay,
        String weeklySummaryTime
) {
}
