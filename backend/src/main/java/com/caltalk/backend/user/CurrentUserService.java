package com.caltalk.backend.user;

import java.time.ZoneId;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.auth.LogoutService;
import com.caltalk.backend.common.error.InvalidTimezoneException;
import com.caltalk.backend.common.error.UnauthorizedCurrentUserException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class CurrentUserService {

    private static final Set<String> REGION_TIMEZONE_IDS = ZoneId.getAvailableZoneIds()
            .stream()
            .filter(timezone -> timezone.contains("/"))
            .collect(Collectors.toUnmodifiableSet());

    private final UserRepository userRepository;
    private final LogoutService logoutService;

    public CurrentUserService(UserRepository userRepository, LogoutService logoutService) {
        this.userRepository = userRepository;
        this.logoutService = logoutService;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        User user = findCurrentUser(authentication, request, response);
        return toResponse(user);
    }

    @Transactional
    public CurrentUserResponse updateTimezone(
            Authentication authentication,
            UpdateTimezoneRequest updateRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        User user = findCurrentUser(authentication, request, response);
        String timezone = updateRequest.timezone();
        if (!REGION_TIMEZONE_IDS.contains(timezone)) {
            throw new InvalidTimezoneException();
        }

        user.changeTimezone(timezone);
        return toResponse(user);
    }

    @Transactional
    public CurrentUserResponse updateChatPreferences(
            Authentication authentication,
            UpdateChatPreferencesRequest updateRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        User user = findCurrentUser(authentication, request, response);
        String style = normalized(updateRequest.replyStyle());
        String density = normalized(updateRequest.replyDensity());
        String layout = normalized(updateRequest.replyLayout());
        String emojiLevel = normalized(updateRequest.emojiLevel());
        String timeFormat = normalized(updateRequest.timeFormat());
        String queryRange = normalized(updateRequest.defaultQueryRange());
        var reminderMinutes = updateRequest.defaultReminderMinutes();
        if (!Set.of("CONCISE", "STANDARD", "ASSISTANT", "BUSINESS", "FRIENDLY").contains(style)
                || !Set.of("ESSENTIAL", "STANDARD", "DETAILED").contains(density)
                || !Set.of("COMPACT", "BALANCED", "SECTIONED").contains(layout)
                || !Set.of("NONE", "MINIMAL", "BALANCED").contains(emojiLevel)
                || !Set.of("TWELVE_HOUR", "TWENTY_FOUR_HOUR").contains(timeFormat)
                || !Set.of(30, 60, 120).contains(updateRequest.defaultDurationMinutes())
                || reminderMinutes.stream().anyMatch(value -> !Set.of(60, 1440, 4320, 10080).contains(value))
                || reminderMinutes.stream().distinct().count() != reminderMinutes.size()
                || !Set.of("TODAY", "THREE_DAYS", "THIS_WEEK", "NEXT_FIVE").contains(queryRange)) {
            throw new IllegalArgumentException("지원하지 않는 카카오톡 답장 설정입니다.");
        }
        user.changeChatPreferences(style, density, layout, emojiLevel, timeFormat,
                updateRequest.confirmCreate(), updateRequest.confirmUpdate(),
                updateRequest.defaultDurationMinutes(), reminderMinutes, queryRange);
        LocalTime dailyTime = parseTime(updateRequest.dailySummaryTime(), user.getDailySummaryTime());
        LocalTime weeklyTime = parseTime(updateRequest.weeklySummaryTime(), user.getWeeklySummaryTime());
        int weeklyDay = updateRequest.weeklySummaryDay() == null ? user.getWeeklySummaryDay() : updateRequest.weeklySummaryDay();
        if (weeklyDay < 1 || weeklyDay > 7) throw new IllegalArgumentException("지원하지 않는 주간 요약 요일입니다.");
        user.changeSummaryPreferences(
                updateRequest.dailySummaryEnabled() == null ? user.isDailySummaryEnabled() : updateRequest.dailySummaryEnabled(), dailyTime,
                updateRequest.weeklySummaryEnabled() == null ? user.isWeeklySummaryEnabled() : updateRequest.weeklySummaryEnabled(), weeklyDay, weeklyTime);
        return toResponse(user);
    }

    private static LocalTime parseTime(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) return fallback;
        try { return LocalTime.parse(value); }
        catch (java.time.format.DateTimeParseException exception) { throw new IllegalArgumentException("올바른 요약 알림 시간을 입력해 주세요."); }
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private User findCurrentUser(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof String email)
                || email.isBlank()) {
            throw new UnauthorizedCurrentUserException();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logoutService.logout(request, response);
                    return new UnauthorizedCurrentUserException();
                });
        return user;
    }

    private CurrentUserResponse toResponse(User user) {
        return new CurrentUserResponse(
                user.getEmail(),
                user.getTimezone(),
                user.getCreatedAt(),
                ChatPreferencesResponse.from(user)
        );
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        logoutService.logout(request, response);
    }
}
