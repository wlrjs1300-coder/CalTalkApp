package com.caltalk.backend.ai;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class KoreanDateRangeResolver {
    private static final Pattern ISO_DATE = Pattern.compile("(?<!\\d)(\\d{4}-\\d{1,2}-\\d{1,2})(?!\\d)");
    private static final Pattern KOREAN_DATE = Pattern.compile("(?:(\\d{4})년\\s*)?(\\d{1,2})월\\s*(\\d{1,2})일");
    private static final Map<String, DayOfWeek> WEEKDAYS = Map.of(
            "월요일", DayOfWeek.MONDAY, "화요일", DayOfWeek.TUESDAY, "수요일", DayOfWeek.WEDNESDAY,
            "목요일", DayOfWeek.THURSDAY, "금요일", DayOfWeek.FRIDAY, "토요일", DayOfWeek.SATURDAY,
            "일요일", DayOfWeek.SUNDAY);

    public Optional<ResolvedDateRange> resolve(String expression, ZonedDateTime now) {
        if (expression == null || expression.isBlank()) return Optional.empty();
        String value = expression.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        LocalDate today = now.toLocalDate();

        if (value.contains("앞으로 3일")) {
            return Optional.of(range(today, today.plusDays(3), now.getZone()));
        }
        if (value.contains("앞으로 365일")) {
            return Optional.of(range(today, today.plusDays(365), now.getZone()));
        }

        if (value.contains("이번 주") && weekday(value).isEmpty()) {
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return Optional.of(range(monday, monday.plusWeeks(1), now.getZone()));
        }
        if (value.contains("다음 주") && weekday(value).isEmpty()) {
            LocalDate monday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
            return Optional.of(range(monday, monday.plusWeeks(1), now.getZone()));
        }

        Optional<DayOfWeek> weekday = weekday(value);
        if (weekday.isPresent()) {
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            if (value.contains("다음 주")) monday = monday.plusWeeks(1);
            else if (!value.contains("이번 주")) {
                LocalDate candidate = today.with(TemporalAdjusters.nextOrSame(weekday.get()));
                return Optional.of(day(candidate, now.getZone()));
            }
            return Optional.of(day(monday.with(TemporalAdjusters.nextOrSame(weekday.get())), now.getZone()));
        }

        if (value.contains("모레")) return Optional.of(day(today.plusDays(2), now.getZone()));
        if (value.contains("내일")) return Optional.of(day(today.plusDays(1), now.getZone()));
        if (value.contains("오늘")) return Optional.of(day(today, now.getZone()));

        Matcher iso = ISO_DATE.matcher(value);
        if (iso.find()) {
            try {
                LocalDate date = LocalDate.parse(iso.group(1), DateTimeFormatter.ofPattern("yyyy-M-d"));
                return Optional.of(day(date, now.getZone()));
            } catch (DateTimeParseException ignored) {
                return Optional.empty();
            }
        }

        Matcher korean = KOREAN_DATE.matcher(value);
        if (korean.find()) {
            int year = korean.group(1) == null ? today.getYear() : Integer.parseInt(korean.group(1));
            try {
                LocalDate date = LocalDate.of(year, Integer.parseInt(korean.group(2)), Integer.parseInt(korean.group(3)));
                if (korean.group(1) == null && date.isBefore(today)) date = date.plusYears(1);
                return Optional.of(day(date, now.getZone()));
            } catch (RuntimeException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static Optional<DayOfWeek> weekday(String value) {
        return WEEKDAYS.entrySet().stream().filter(entry -> value.contains(entry.getKey()))
                .map(Map.Entry::getValue).findFirst();
    }

    private static ResolvedDateRange day(LocalDate date, ZoneId zone) {
        return range(date, date.plusDays(1), zone);
    }

    private static ResolvedDateRange range(LocalDate from, LocalDate to, ZoneId zone) {
        return new ResolvedDateRange(from, to, from.atStartOfDay(zone).toInstant(), to.atStartOfDay(zone).toInstant());
    }
}
