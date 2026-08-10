package com.caltalk.backend.notification;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.schedule.Schedule;
import com.caltalk.backend.schedule.ScheduleRepository;

import tools.jackson.databind.ObjectMapper;

@Component
class ScheduleReminderDispatcher {
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("M월 d일 a h:mm");
    private final ScheduleRepository schedules;
    private final PushSubscriptionRepository subscriptions;
    private final ReminderDeliveryRepository deliveries;
    private final WebPushSender sender;
    private final ObjectMapper objectMapper;

    ScheduleReminderDispatcher(ScheduleRepository schedules, PushSubscriptionRepository subscriptions,
            ReminderDeliveryRepository deliveries, WebPushSender sender, ObjectMapper objectMapper) {
        this.schedules = schedules; this.subscriptions = subscriptions; this.deliveries = deliveries;
        this.sender = sender; this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${caltalk.push.dispatch-interval:30000}")
    @Transactional
    void dispatch() {
        if (!sender.available()) return;
        Instant now = Instant.now();
        Instant oldestDue = now.minusSeconds(600);
        for (Schedule schedule : schedules.findUpcomingForReminders(now, now.plusSeconds(10080L * 60L + 60L))) {
            for (int minutes : schedule.getReminderMinutes()) {
                Instant due = schedule.getStartAt().minusSeconds(minutes * 60L);
                if (due.isAfter(now) || !due.isAfter(oldestDue)
                        || deliveries.existsByScheduleIdAndReminderMinutes(schedule.getId(), minutes)) continue;
                boolean sent = sendToAllDevices(schedule, minutes);
                if (sent) deliveries.save(new ReminderDelivery(schedule, minutes, now));
            }
        }
    }

    private boolean sendToAllDevices(Schedule schedule, int minutes) {
        ZoneId zone = ZoneId.of(schedule.getOwner().getTimezone());
        String when = schedule.getStartAt().atZone(zone).format(DISPLAY);
        String body = schedule.getTitle() + "\n" + when;
        String url = "/day/" + schedule.getStartAt().atZone(zone).toLocalDate() + "/event/" + schedule.getId();
        String payload = objectMapper.writeValueAsString(Map.of(
                "title", reminderLabel(minutes), "body", body, "url", url,
                "tag", "schedule-" + schedule.getId() + "-" + minutes));
        boolean sent = false;
        for (PushSubscription subscription : subscriptions.findAllByUser(schedule.getOwner())) {
            try { sender.send(subscription, payload); sent = true; }
            catch (PushSubscriptionExpiredException ignored) { subscriptions.delete(subscription); }
            catch (Exception ignored) { /* retry on the next dispatcher pass */ }
        }
        return sent;
    }

    private static String reminderLabel(int minutes) {
        return switch (minutes) {
            case 10080 -> "🔔 일주일 뒤 일정이 있어요";
            case 4320 -> "🔔 3일 뒤 일정이 있어요";
            case 1440 -> "🔔 내일 일정이 있어요";
            default -> "🔔 1시간 뒤 일정이 있어요";
        };
    }
}
