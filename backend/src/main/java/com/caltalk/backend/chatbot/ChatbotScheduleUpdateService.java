package com.caltalk.backend.chatbot;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.schedule.Schedule;
import com.caltalk.backend.schedule.ScheduleRepository;
import com.caltalk.backend.schedule.history.ScheduleChangeHistory;
import com.caltalk.backend.schedule.history.ScheduleChangeHistoryRepository;
import com.caltalk.backend.user.User;

@Service
public class ChatbotScheduleUpdateService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("M월 d일 HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private final ScheduleRepository schedules;
    private final ScheduleChangeHistoryRepository history;

    public ChatbotScheduleUpdateService(ScheduleRepository schedules, ScheduleChangeHistoryRepository history) {
        this.schedules = schedules;
        this.history = history;
    }

    @Transactional
    public String update(User user, ChatbotCommandStateStore.PendingUpdate pending) {
        Schedule schedule = schedules.findByIdAndOwner(pending.scheduleId(), user).orElse(null);
        if (schedule == null) return "이미 삭제되었거나 찾을 수 없는 일정이에요.";
        if (!schedule.getVersion().equals(pending.version())) {
            return "일정이 그 사이 변경되어 수정하지 않았어요. 다시 요청해 주세요.";
        }
        Instant newStart = Instant.parse(pending.newStartAt());
        Instant newEnd = Instant.parse(pending.newEndAt());
        if (!newEnd.isAfter(newStart)) return "종료 시간은 시작 시간보다 뒤여야 해요.";
        List<Schedule> conflicts = schedules.findConflictsExcluding(user, schedule.getId(), newStart, newEnd);
        if (!conflicts.isEmpty()) {
            return "같은 시간에 '%s' 일정이 있어 수정하지 않았어요.".formatted(conflicts.getFirst().getTitle());
        }
        String oldTitle = schedule.getTitle();
        String oldLocation = schedule.getLocation();
        Instant oldStart = schedule.getStartAt();
        Instant oldEnd = schedule.getEndAt();
        schedule.update(oldTitle, oldLocation, newStart, newEnd);
        schedules.saveAndFlush(schedule);
        history.save(ScheduleChangeHistory.updated(
                schedule, user, oldTitle, oldStart, oldEnd, oldLocation, "KAKAO"));
        ZoneId zone = safeZone(user.getTimezone());
        return "'%s' 일정을 %s부터 %s까지로 변경했어요.".formatted(
                schedule.getTitle(), newStart.atZone(zone).format(DATE_TIME), newEnd.atZone(zone).format(TIME));
    }

    private static ZoneId safeZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (RuntimeException ignored) {
            return ZoneId.of("Asia/Seoul");
        }
    }
}
