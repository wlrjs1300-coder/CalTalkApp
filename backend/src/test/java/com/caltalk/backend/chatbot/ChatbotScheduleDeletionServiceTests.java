package com.caltalk.backend.chatbot;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.caltalk.backend.confirmation.ConfirmationService;
import com.caltalk.backend.schedule.Schedule;
import com.caltalk.backend.schedule.ScheduleRepository;
import com.caltalk.backend.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatbotScheduleDeletionServiceTests {
    private final User user = new User("user@example.com", "hash");

    @Test
    void deletesOnlyMatchingOwnedVersion() {
        ScheduleRepository schedules = mock(ScheduleRepository.class);
        ConfirmationService confirmations = mock(ConfirmationService.class);
        Schedule schedule = mock(Schedule.class);
        when(schedule.getId()).thenReturn(57L);
        when(schedule.getVersion()).thenReturn(2L);
        when(schedule.getTitle()).thenReturn("테스트 회의");
        when(schedules.findByIdAndOwner(57L, user)).thenReturn(Optional.of(schedule));
        ChatbotScheduleDeletionService service = new ChatbotScheduleDeletionService(schedules, confirmations);

        String result = service.delete(user,
                new ChatbotCommandStateStore.PendingDelete(57L, 2L, "테스트 회의", "내일"));

        assertThat(result).isEqualTo("'테스트 회의' 일정을 삭제했어요.");
        verify(confirmations).detachPendingUpdates(57L);
        verify(schedules).delete(schedule);
        verify(schedules).flush();
    }

    @Test
    void refusesDeleteWhenVersionChanged() {
        ScheduleRepository schedules = mock(ScheduleRepository.class);
        ConfirmationService confirmations = mock(ConfirmationService.class);
        Schedule schedule = mock(Schedule.class);
        when(schedule.getVersion()).thenReturn(3L);
        when(schedules.findByIdAndOwner(57L, user)).thenReturn(Optional.of(schedule));
        ChatbotScheduleDeletionService service = new ChatbotScheduleDeletionService(schedules, confirmations);

        String result = service.delete(user,
                new ChatbotCommandStateStore.PendingDelete(57L, 2L, "테스트 회의", "내일"));

        assertThat(result).contains("변경되어 삭제하지 않았어요");
        verify(schedules, never()).delete(schedule);
    }
}
