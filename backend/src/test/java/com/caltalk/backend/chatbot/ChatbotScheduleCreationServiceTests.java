package com.caltalk.backend.chatbot;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.caltalk.backend.ai.CommandStatus;
import com.caltalk.backend.ai.KoreanDateRangeResolver;
import com.caltalk.backend.ai.ScheduleCommand;
import com.caltalk.backend.ai.ScheduleIntent;
import com.caltalk.backend.schedule.Schedule;
import com.caltalk.backend.schedule.ScheduleRepository;
import com.caltalk.backend.schedule.history.ScheduleChangeHistoryRepository;
import com.caltalk.backend.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatbotScheduleCreationServiceTests {
    private final ZoneId zone = ZoneId.of("Asia/Seoul");
    private final ZonedDateTime now = ZonedDateTime.of(2026, 8, 4, 14, 0, 0, 0, zone);
    private final User user = new User("user@example.com", "hash");

    @Test
    void createsOnlyAfterValidatedCommandAndWritesKakaoHistory() {
        ScheduleRepository schedules = mock(ScheduleRepository.class);
        ScheduleChangeHistoryRepository history = mock(ScheduleChangeHistoryRepository.class);
        when(schedules.findConflicts(eq(user), any(), any())).thenReturn(List.of());
        when(schedules.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ChatbotScheduleCreationService service = new ChatbotScheduleCreationService(
                new KoreanDateRangeResolver(), schedules, history);

        ChatbotScheduleCreationService.CreationResult result = service.create(user, zone, now, command());

        assertThat(result.status()).isEqualTo(ChatbotScheduleCreationService.Status.CREATED);
        assertThat(result.message()).contains("팀 미팅", "8월 5일", "15:00", "16:00");
        verify(schedules).saveAndFlush(any(Schedule.class));
        verify(history).save(any());
    }

    @Test
    void blocksCreationWhenScheduleConflicts() {
        ScheduleRepository schedules = mock(ScheduleRepository.class);
        ScheduleChangeHistoryRepository history = mock(ScheduleChangeHistoryRepository.class);
        when(schedules.findConflicts(eq(user), any(), any())).thenReturn(List.of(new Schedule(
                user, "기존 회의", null, now.toInstant(), now.plusHours(1).toInstant())));
        ChatbotScheduleCreationService service = new ChatbotScheduleCreationService(
                new KoreanDateRangeResolver(), schedules, history);

        ChatbotScheduleCreationService.CreationResult result = service.create(user, zone, now, command());

        assertThat(result.status()).isEqualTo(ChatbotScheduleCreationService.Status.CONFLICT);
        assertThat(result.message()).contains("기존 회의", "등록하지 않았어요");
        verify(schedules, never()).saveAndFlush(any());
        verify(history, never()).save(any());
    }

    private static ScheduleCommand command() {
        return new ScheduleCommand("1.0", ScheduleIntent.CREATE_EVENT, CommandStatus.READY,
                "팀 미팅", "내일", "15:00", "16:00", null, List.of(), List.of(), null);
    }
}
