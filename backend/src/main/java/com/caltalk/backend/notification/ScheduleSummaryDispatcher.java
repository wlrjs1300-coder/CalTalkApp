package com.caltalk.backend.notification;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.schedule.Schedule;
import com.caltalk.backend.schedule.ScheduleRepository;
import com.caltalk.backend.user.User;
import com.caltalk.backend.user.UserRepository;

import tools.jackson.databind.ObjectMapper;

@Component
class ScheduleSummaryDispatcher {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("a h:mm");
    private final UserRepository users;
    private final ScheduleRepository schedules;
    private final PushSubscriptionRepository subscriptions;
    private final SummaryDeliveryRepository deliveries;
    private final WebPushSender sender;
    private final ObjectMapper objectMapper;

    ScheduleSummaryDispatcher(UserRepository users, ScheduleRepository schedules,
            PushSubscriptionRepository subscriptions, SummaryDeliveryRepository deliveries,
            WebPushSender sender, ObjectMapper objectMapper) {
        this.users = users; this.schedules = schedules; this.subscriptions = subscriptions;
        this.deliveries = deliveries; this.sender = sender; this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${caltalk.push.summary-dispatch-interval:30000}")
    @Transactional
    void dispatch() {
        if (!sender.available()) return;
        Instant now = Instant.now();
        for (User user : users.findAll()) {
            ZonedDateTime localNow = now.atZone(java.time.ZoneId.of(user.getTimezone()));
            if (user.isDailySummaryEnabled() && !localNow.toLocalTime().isBefore(user.getDailySummaryTime())) {
                send(user, "DAILY", localNow.toLocalDate(), localNow.toLocalDate().plusDays(1), now);
            }
            if (user.isWeeklySummaryEnabled() && localNow.getDayOfWeek().getValue() == user.getWeeklySummaryDay()
                    && !localNow.toLocalTime().isBefore(user.getWeeklySummaryTime())) {
                LocalDate monday = user.getWeeklySummaryDay() == DayOfWeek.SUNDAY.getValue()
                        ? localNow.toLocalDate().plusDays(1)
                        : localNow.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                send(user, "WEEKLY", monday, monday.plusDays(7), now);
            }
        }
    }

    private void send(User user, String type, LocalDate fromDate, LocalDate toDate, Instant now) {
        if (deliveries.existsByUserIdAndTypeAndPeriodStart(user.getId(), type, fromDate)) return;
        var zone = java.time.ZoneId.of(user.getTimezone());
        List<Schedule> found = schedules.findInRange(user, fromDate.atStartOfDay(zone).toInstant(), toDate.atStartOfDay(zone).toInstant());
        String title = type.equals("DAILY") ? "오늘 일정 요약" : "이번 주 일정 요약";
        String body = summaryBody(found, zone);
        String url = type.equals("DAILY") ? "/day/" + fromDate : "/";
        String payload = objectMapper.writeValueAsString(Map.of("title", title, "body", body, "url", url,
                "tag", "summary-" + type.toLowerCase() + "-" + fromDate));
        boolean sent = false;
        for (PushSubscription subscription : subscriptions.findAllByUser(user)) {
            try { sender.send(subscription, payload); sent = true; }
            catch (PushSubscriptionExpiredException ignored) { subscriptions.delete(subscription); }
            catch (Exception ignored) { }
        }
        if (sent) deliveries.save(new SummaryDelivery(user, type, fromDate, now));
    }

    private static String summaryBody(List<Schedule> found, java.time.ZoneId zone) {
        if (found.isEmpty()) return "등록된 일정이 없어요.";
        StringBuilder body = new StringBuilder(found.size() + "개의 일정이 있어요.");
        found.stream().limit(3).forEach(schedule -> body.append("\n• ")
                .append(schedule.getStartAt().atZone(zone).format(TIME)).append("  ").append(schedule.getTitle()));
        if (found.size() > 3) body.append("\n외 ").append(found.size() - 3).append("개 일정");
        return body.toString();
    }
}
