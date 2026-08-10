package com.caltalk.backend.user;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    private static final String DEFAULT_TIMEZONE = "Asia/Seoul";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", length = 60)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(name = "chat_reply_style", nullable = false, length = 20)
    private String chatReplyStyle = "STANDARD";

    @Column(name = "chat_reply_density", nullable = false, length = 20)
    private String chatReplyDensity = "STANDARD";

    @Column(name = "chat_reply_layout", nullable = false, length = 20)
    private String chatReplyLayout = "BALANCED";

    @Column(name = "chat_emoji_level", nullable = false, length = 20)
    private String chatEmojiLevel = "MINIMAL";

    @Column(name = "default_reminder_minutes", nullable = false, length = 100)
    private String defaultReminderMinutes = "1440";

    @Column(name = "chat_time_format", nullable = false, length = 20)
    private String chatTimeFormat = "TWELVE_HOUR";

    @Column(name = "chat_confirm_create", nullable = false)
    private boolean chatConfirmCreate = true;

    @Column(name = "chat_confirm_update", nullable = false)
    private boolean chatConfirmUpdate = true;

    @Column(name = "chat_default_duration_minutes", nullable = false)
    private int chatDefaultDurationMinutes = 60;

    @Column(name = "chat_default_query_range", nullable = false, length = 20)
    private String chatDefaultQueryRange = "TODAY";

    @Column(name = "daily_summary_enabled", nullable = false)
    private boolean dailySummaryEnabled;
    @Column(name = "daily_summary_time", nullable = false)
    private LocalTime dailySummaryTime = LocalTime.of(8, 0);
    @Column(name = "weekly_summary_enabled", nullable = false)
    private boolean weeklySummaryEnabled;
    @Column(name = "weekly_summary_day", nullable = false)
    private int weeklySummaryDay = 1;
    @Column(name = "weekly_summary_time", nullable = false)
    private LocalTime weeklySummaryTime = LocalTime.of(18, 0);

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.timezone = DEFAULT_TIMEZONE;
    }

    @PrePersist
    void assignCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getEmail() {
        return email;
    }

    public Long getId() {
        return id;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getTimezone() {
        return timezone;
    }

    public void changeTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getChatReplyStyle() { return chatReplyStyle; }
    public String getChatReplyDensity() { return chatReplyDensity; }
    public String getChatReplyLayout() { return chatReplyLayout; }
    public String getChatEmojiLevel() { return chatEmojiLevel; }
    public List<Integer> getDefaultReminderMinutes() {
        if (defaultReminderMinutes == null || defaultReminderMinutes.isBlank()) return List.of();
        return java.util.Arrays.stream(defaultReminderMinutes.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).map(Integer::valueOf).toList();
    }
    public String getChatTimeFormat() { return chatTimeFormat; }
    public boolean isChatConfirmCreate() { return chatConfirmCreate; }
    public boolean isChatConfirmUpdate() { return chatConfirmUpdate; }
    public int getChatDefaultDurationMinutes() { return chatDefaultDurationMinutes; }
    public String getChatDefaultQueryRange() { return chatDefaultQueryRange; }
    public boolean isDailySummaryEnabled() { return dailySummaryEnabled; }
    public LocalTime getDailySummaryTime() { return dailySummaryTime; }
    public boolean isWeeklySummaryEnabled() { return weeklySummaryEnabled; }
    public int getWeeklySummaryDay() { return weeklySummaryDay; }
    public LocalTime getWeeklySummaryTime() { return weeklySummaryTime; }

    public void changeSummaryPreferences(boolean dailyEnabled, LocalTime dailyTime,
            boolean weeklyEnabled, int weeklyDay, LocalTime weeklyTime) {
        this.dailySummaryEnabled = dailyEnabled;
        this.dailySummaryTime = dailyTime;
        this.weeklySummaryEnabled = weeklyEnabled;
        this.weeklySummaryDay = weeklyDay;
        this.weeklySummaryTime = weeklyTime;
    }

    public void changeChatPreferences(String replyStyle, String replyDensity, String replyLayout, String emojiLevel, String timeFormat,
            boolean confirmCreate, boolean confirmUpdate, int defaultDurationMinutes, List<Integer> reminderMinutes,
            String defaultQueryRange) {
        this.chatReplyStyle = replyStyle;
        this.chatReplyDensity = replyDensity;
        this.chatReplyLayout = replyLayout;
        this.chatEmojiLevel = emojiLevel;
        this.chatTimeFormat = timeFormat;
        this.chatConfirmCreate = confirmCreate;
        this.chatConfirmUpdate = confirmUpdate;
        this.chatDefaultDurationMinutes = defaultDurationMinutes;
        this.defaultReminderMinutes = reminderMinutes.stream().sorted(java.util.Comparator.reverseOrder())
                .map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        this.chatDefaultQueryRange = defaultQueryRange;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
