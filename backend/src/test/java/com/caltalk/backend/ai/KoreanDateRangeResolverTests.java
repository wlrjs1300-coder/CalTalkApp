package com.caltalk.backend.ai;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KoreanDateRangeResolverTests {
    private final KoreanDateRangeResolver resolver = new KoreanDateRangeResolver();
    private final ZonedDateTime now = ZonedDateTime.of(2026, 8, 4, 14, 0, 0, 0, ZoneId.of("Asia/Seoul"));

    @Test
    void resolvesRelativeDaysInUserTimezone() {
        assertThat(resolver.resolve("내일", now).orElseThrow().fromDate()).hasToString("2026-08-05");
        assertThat(resolver.resolve("모레 일정", now).orElseThrow().fromDate()).hasToString("2026-08-06");
    }

    @Test
    void resolvesWeekdayWithExplicitWeek() {
        assertThat(resolver.resolve("이번 주 금요일", now).orElseThrow().fromDate()).hasToString("2026-08-07");
        assertThat(resolver.resolve("다음 주 월요일", now).orElseThrow().fromDate()).hasToString("2026-08-10");
    }

    @Test
    void rollsMonthAndDayWithoutYearIntoNextYearWhenAlreadyPassed() {
        assertThat(resolver.resolve("3월 2일", now).orElseThrow().fromDate()).hasToString("2027-03-02");
    }

    @Test
    void rejectsUnknownDateExpression() {
        assertThat(resolver.resolve("시간 될 때", now)).isEmpty();
    }
}
