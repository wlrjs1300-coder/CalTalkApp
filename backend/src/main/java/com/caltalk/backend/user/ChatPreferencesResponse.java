package com.caltalk.backend.user;

import java.util.List;

public record ChatPreferencesResponse(
        String replyStyle,
        String replyDensity,
        String replyLayout,
        String emojiLevel,
        String timeFormat,
        boolean confirmCreate,
        boolean confirmUpdate,
        int defaultDurationMinutes,
        List<Integer> defaultReminderMinutes,
        String defaultQueryRange,
        boolean dailySummaryEnabled,
        String dailySummaryTime,
        boolean weeklySummaryEnabled,
        int weeklySummaryDay,
        String weeklySummaryTime
) {
    static ChatPreferencesResponse from(User user) {
        return new ChatPreferencesResponse(
                user.getChatReplyStyle(), user.getChatReplyDensity(), user.getChatReplyLayout(), user.getChatEmojiLevel(), user.getChatTimeFormat(),
                user.isChatConfirmCreate(), user.isChatConfirmUpdate(),
                user.getChatDefaultDurationMinutes(), user.getDefaultReminderMinutes(), user.getChatDefaultQueryRange(),
                user.isDailySummaryEnabled(), user.getDailySummaryTime().toString(),
                user.isWeeklySummaryEnabled(), user.getWeeklySummaryDay(), user.getWeeklySummaryTime().toString());
    }
}
