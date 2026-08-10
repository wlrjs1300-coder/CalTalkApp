package com.caltalk.backend.chatbot;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.ai.KoreanDateRangeResolver;
import com.caltalk.backend.ai.ResolvedDateRange;
import com.caltalk.backend.ai.ScheduleCommand;
import com.caltalk.backend.schedule.Schedule;
import com.caltalk.backend.schedule.ScheduleRepository;
import com.caltalk.backend.schedule.history.ScheduleChangeHistory;
import com.caltalk.backend.schedule.history.ScheduleChangeHistoryRepository;
import com.caltalk.backend.user.User;

@Service
public class ChatbotScheduleCreationService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm");
    private static final Pattern KOREAN_TIME = Pattern.compile("(오전|오후)?\\s*(\\d{1,2})시(?:\\s*(\\d{1,2})분)?");
    private final KoreanDateRangeResolver dateResolver;
    private final ScheduleRepository schedules;
    private final ScheduleChangeHistoryRepository history;

    public ChatbotScheduleCreationService(KoreanDateRangeResolver dateResolver, ScheduleRepository schedules,
            ScheduleChangeHistoryRepository history) {
        this.dateResolver = dateResolver;
        this.schedules = schedules;
        this.history = history;
    }

    @Transactional
    public CreationResult create(User user, ZoneId zone, ZonedDateTime now, ScheduleCommand command) {
        ResolvedDateRange date = dateResolver.resolve(command.dateExpression(), now).orElse(null);
        LocalTime start = parseTime(command.startTime());
        LocalTime end = parseTime(command.endTime());
        if (date == null || start == null || end == null) return CreationResult.invalid();
        ZonedDateTime startAt = date.fromDate().atTime(start).atZone(zone);
        ZonedDateTime endAt = date.fromDate().atTime(end).atZone(zone);
        if (!endAt.isAfter(startAt)) return CreationResult.invalid();
        List<Schedule> conflicts = schedules.findConflicts(user, startAt.toInstant(), endAt.toInstant());
        if (!conflicts.isEmpty()) return CreationResult.conflict(conflicts.getFirst().getTitle());
        Schedule schedule = new Schedule(user, command.title(), null, startAt.toInstant(), endAt.toInstant());
        schedule.changeReminderMinutes(user.getDefaultReminderMinutes());
        Schedule saved = schedules.saveAndFlush(schedule);
        history.save(ScheduleChangeHistory.created(saved, user, "KAKAO"));
        return CreationResult.created(command.title(), startAt, endAt);
    }

    private static LocalTime parseTime(String value) {
        if (value == null) return null;
        try {
            return LocalTime.parse(value.trim(), TIME);
        } catch (DateTimeParseException ignored) {
            Matcher matcher = KOREAN_TIME.matcher(value.trim());
            if (!matcher.matches()) return null;
            int hour = Integer.parseInt(matcher.group(2));
            int minute = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
            if (hour > 12 || minute > 59) return null;
            if ("오후".equals(matcher.group(1)) && hour < 12) hour += 12;
            if ("오전".equals(matcher.group(1)) && hour == 12) hour = 0;
            try {
                return LocalTime.of(hour, minute);
            } catch (RuntimeException invalidTime) {
                return null;
            }
        }
    }

    public record CreationResult(Status status, String message) {
        static CreationResult created(String title, ZonedDateTime start, ZonedDateTime end) {
            return new CreationResult(Status.CREATED, "%s 일정을 %d월 %d일 %s부터 %s까지 등록했어요.".formatted(
                    title, start.getMonthValue(), start.getDayOfMonth(), start.toLocalTime(), end.toLocalTime()));
        }
        static CreationResult conflict(String title) {
            return new CreationResult(Status.CONFLICT, "같은 시간에 '%s' 일정이 있어 등록하지 않았어요.".formatted(title));
        }
        static CreationResult invalid() {
            return new CreationResult(Status.INVALID, "날짜나 시간을 정확히 해석하지 못했어요. 다시 말씀해 주세요.");
        }
    }

    public enum Status { CREATED, CONFLICT, INVALID }
}
