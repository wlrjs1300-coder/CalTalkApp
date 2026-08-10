package com.caltalk.backend.chatbot;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.caltalk.backend.ai.CommandStatus;
import com.caltalk.backend.ai.KoreanDateRangeResolver;
import com.caltalk.backend.ai.NaturalLanguageProvider;
import com.caltalk.backend.ai.ScheduleCommand;
import com.caltalk.backend.ai.ScheduleIntent;
import com.caltalk.backend.kakao.KakaoLinkService;
import com.caltalk.backend.schedule.ScheduleRepository;
import com.caltalk.backend.schedule.Schedule;
import com.caltalk.backend.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class KakaoScheduleAssistantServiceTests {

    @Test
    void answersLinkedUsersScheduleQueryWithoutChangingData() {
        KakaoLinkService links = mock(KakaoLinkService.class);
        NaturalLanguageProvider provider = mock(NaturalLanguageProvider.class);
        ScheduleRepository schedules = mock(ScheduleRepository.class);
        User user = new User("user@example.com", "hash");
        user.changeChatPreferences("STANDARD", "STANDARD", "BALANCED", "NONE", "TWELVE_HOUR", true, true, 60, List.of(1440), "TODAY");
        when(links.linkedUser("kakao-user")).thenReturn(Optional.of(user));
        when(provider.analyze(eq("내일 일정 알려줘"), any())).thenReturn(new ScheduleCommand(
                "1.0", ScheduleIntent.SEARCH_EVENTS, CommandStatus.READY, null, "내일", null,
                null, null, List.of(), List.of(), null));
        when(schedules.findInRange(eq(user), any(), any())).thenReturn(List.of());
        Clock clock = Clock.fixed(Instant.parse("2026-08-04T05:00:00Z"), ZoneOffset.UTC);
        KakaoScheduleAssistantService service = new KakaoScheduleAssistantService(links, provider,
                new KoreanDateRangeResolver(), schedules, mock(ChatbotCommandStateStore.class),
                mock(ChatbotScheduleCreationService.class), mock(ChatbotScheduleDeletionService.class),
                mock(ChatbotScheduleUpdateService.class), clock);

        assertThat(service.reply("kakao-user", "내일 일정 알려줘"))
                .isEqualTo("8월 5일에는 등록된 일정이 없어요.");
    }

    @Test
    void usesDefaultTodayRangeWhenSearchDateIsOmitted() {
        KakaoLinkService links = mock(KakaoLinkService.class);
        NaturalLanguageProvider provider = mock(NaturalLanguageProvider.class);
        ScheduleRepository schedules = mock(ScheduleRepository.class);
        User user = new User("user@example.com", "hash");
        when(links.linkedUser("kakao-user")).thenReturn(Optional.of(user));
        when(provider.analyze(eq("일정 알려줘"), any())).thenReturn(new ScheduleCommand(
                "1.0", ScheduleIntent.SEARCH_EVENTS, CommandStatus.NEEDS_CLARIFICATION, null, null, null,
                null, null, List.of("date"), List.of(), "어느 날짜의 일정을 확인할까요?"));
        when(schedules.findInRange(eq(user), any(), any())).thenReturn(List.of());
        KakaoScheduleAssistantService service = new KakaoScheduleAssistantService(links, provider,
                new KoreanDateRangeResolver(), schedules, mock(ChatbotCommandStateStore.class),
                mock(ChatbotScheduleCreationService.class), mock(ChatbotScheduleDeletionService.class),
                mock(ChatbotScheduleUpdateService.class), Clock.systemUTC());

        assertThat(service.reply("kakao-user", "일정 알려줘"))
                .contains("등록된 일정이 없어요.");
    }

    @Test
    void formatsScheduleQueriesDifferentlyByReplyDensity() {
        KakaoLinkService links = mock(KakaoLinkService.class);
        NaturalLanguageProvider provider = mock(NaturalLanguageProvider.class);
        ScheduleRepository schedules = mock(ScheduleRepository.class);
        User user = new User("user@example.com", "hash");
        when(links.linkedUser("kakao-user")).thenReturn(Optional.of(user));
        when(provider.analyze(eq("내일 일정 알려줘"), any())).thenReturn(new ScheduleCommand(
                "1.0", ScheduleIntent.SEARCH_EVENTS, CommandStatus.READY, null, "내일", null,
                null, null, List.of(), List.of(), null));
        Schedule schedule = new Schedule(user, "문단 테스트 회의", "강남역",
                Instant.parse("2026-08-05T05:00:00Z"), Instant.parse("2026-08-05T06:00:00Z"));
        when(schedules.findInRange(eq(user), any(), any())).thenReturn(List.of(schedule));
        KakaoScheduleAssistantService service = new KakaoScheduleAssistantService(links, provider,
                new KoreanDateRangeResolver(), schedules, mock(ChatbotCommandStateStore.class),
                mock(ChatbotScheduleCreationService.class), mock(ChatbotScheduleDeletionService.class),
                mock(ChatbotScheduleUpdateService.class),
                Clock.fixed(Instant.parse("2026-08-04T05:00:00Z"), ZoneOffset.UTC));

        user.changeChatPreferences("STANDARD", "ESSENTIAL", "BALANCED", "NONE", "TWELVE_HOUR", true, true, 60, List.of(1440), "TODAY");
        assertThat(service.reply("kakao-user", "내일 일정 알려줘"))
                .isEqualTo("8월 5일 · 1개\n• 오후 2:00  문단 테스트 회의");

        user.changeChatPreferences("STANDARD", "STANDARD", "BALANCED", "NONE", "TWELVE_HOUR", true, true, 60, List.of(1440), "TODAY");
        assertThat(service.reply("kakao-user", "내일 일정 알려줘"))
                .isEqualTo("8월 5일 일정이에요.\n• 오후 2:00~오후 3:00  문단 테스트 회의");

        user.changeChatPreferences("STANDARD", "DETAILED", "BALANCED", "NONE", "TWELVE_HOUR", true, true, 60, List.of(1440), "TODAY");
        assertThat(service.reply("kakao-user", "내일 일정 알려줘"))
                .isEqualTo("8월 5일 일정이에요.\n• 오후 2:00~오후 3:00  문단 테스트 회의\n  장소 · 강남역");

        user.changeChatPreferences("STANDARD", "STANDARD", "COMPACT", "NONE", "TWELVE_HOUR", true, true, 60, List.of(1440), "TODAY");
        assertThat(service.reply("kakao-user", "내일 일정 알려줘"))
                .isEqualTo("8월 5일 · 1개\n• 오후 2:00~오후 3:00  문단 테스트 회의");

        user.changeChatPreferences("STANDARD", "STANDARD", "SECTIONED", "NONE", "TWELVE_HOUR", true, true, 60, List.of(1440), "TODAY");
        assertThat(service.reply("kakao-user", "내일 일정 알려줘"))
                .isEqualTo("8월 5일 일정이에요.\n• 일정 1\n  문단 테스트 회의\n  시간 · 오후 2:00~오후 3:00");

        user.changeChatPreferences("STANDARD", "DETAILED", "SECTIONED", "BALANCED", "TWELVE_HOUR", true, true, 60, List.of(1440), "TODAY");
        assertThat(service.reply("kakao-user", "내일 일정 알려줘"))
                .isEqualTo("🗓️ 8월 5일 일정이에요.\n• 일정 1\n  문단 테스트 회의\n  🕒 시간 · 오후 2:00~오후 3:00\n  📍 장소 · 강남역");
    }

    @Test
    void storesCompleteCreateCommandInsteadOfWritingImmediately() {
        KakaoLinkService links = mock(KakaoLinkService.class);
        NaturalLanguageProvider provider = mock(NaturalLanguageProvider.class);
        ScheduleRepository schedules = mock(ScheduleRepository.class);
        ChatbotCommandStateStore states = mock(ChatbotCommandStateStore.class);
        ChatbotScheduleCreationService creation = mock(ChatbotScheduleCreationService.class);
        User user = new User("user@example.com", "hash");
        ScheduleCommand command = createCommand();
        when(links.linkedUser("kakao-user")).thenReturn(Optional.of(user));
        when(links.protectedExternalUserKey("kakao-user")).thenReturn("protected-key");
        when(provider.analyze(eq("내일 3시부터 4시까지 팀 미팅 추가해줘"), any())).thenReturn(command);
        KakaoScheduleAssistantService service = new KakaoScheduleAssistantService(links, provider,
                new KoreanDateRangeResolver(), schedules, states, creation,
                mock(ChatbotScheduleDeletionService.class), mock(ChatbotScheduleUpdateService.class), Clock.systemUTC());

        String reply = service.reply("kakao-user", "내일 3시부터 4시까지 팀 미팅 추가해줘");

        assertThat(reply).contains("팀 미팅", "확인", "취소");
        verify(states).save("protected-key", command);
        verify(creation, org.mockito.Mockito.never()).create(any(), any(), any(), any());
    }

    @Test
    void createsPendingCommandOnlyWhenUserConfirms() {
        KakaoLinkService links = mock(KakaoLinkService.class);
        NaturalLanguageProvider provider = mock(NaturalLanguageProvider.class);
        ScheduleRepository schedules = mock(ScheduleRepository.class);
        ChatbotCommandStateStore states = mock(ChatbotCommandStateStore.class);
        ChatbotScheduleCreationService creation = mock(ChatbotScheduleCreationService.class);
        User user = new User("user@example.com", "hash");
        user.changeChatPreferences("STANDARD", "STANDARD", "BALANCED", "NONE", "TWELVE_HOUR", true, true, 60, List.of(1440), "TODAY");
        ScheduleCommand command = createCommand();
        when(links.linkedUser("kakao-user")).thenReturn(Optional.of(user));
        when(links.protectedExternalUserKey("kakao-user")).thenReturn("protected-key");
        when(states.get("protected-key")).thenReturn(Optional.of(command));
        when(creation.create(eq(user), any(), any(), eq(command))).thenReturn(
                new ChatbotScheduleCreationService.CreationResult(
                        ChatbotScheduleCreationService.Status.CREATED, "등록했어요."));
        KakaoScheduleAssistantService service = new KakaoScheduleAssistantService(links, provider,
                new KoreanDateRangeResolver(), schedules, states, creation,
                mock(ChatbotScheduleDeletionService.class), mock(ChatbotScheduleUpdateService.class), Clock.systemUTC());

        assertThat(service.reply("kakao-user", "확인")).isEqualTo("등록했어요.");
        verify(states).delete("protected-key");
        verify(creation).create(eq(user), any(), any(), eq(command));
    }

    @Test
    void selectsSecondDeleteCandidateByOrdinalBeforeConfirmation() {
        KakaoLinkService links = mock(KakaoLinkService.class);
        NaturalLanguageProvider provider = mock(NaturalLanguageProvider.class);
        ChatbotCommandStateStore states = mock(ChatbotCommandStateStore.class);
        User user = new User("user@example.com", "hash");
        ChatbotCommandStateStore.PendingSelection selection = new ChatbotCommandStateStore.PendingSelection(
                ChatbotCommandStateStore.SelectionAction.DELETE,
                List.of(candidate(11L, "팀 회의", "2026-08-05T01:00:00Z", "2026-08-05T02:00:00Z"),
                        candidate(12L, "팀 회의", "2026-08-05T06:00:00Z", "2026-08-05T07:00:00Z")),
                null, null, "내일");
        when(links.linkedUser("kakao-user")).thenReturn(Optional.of(user));
        when(links.protectedExternalUserKey("kakao-user")).thenReturn("protected-key");
        when(states.getSelection("protected-key")).thenReturn(Optional.of(selection));
        KakaoScheduleAssistantService service = service(links, provider, states);

        String reply = service.reply("kakao-user", "두 번째");

        assertThat(reply).contains("팀 회의", "삭제할까요", "확인");
        verify(states).deleteSelection("protected-key");
        verify(states).saveDelete(eq("protected-key"), eq(new ChatbotCommandStateStore.PendingDelete(
                12L, 1L, "팀 회의", "내일")));
        verify(provider, org.mockito.Mockito.never()).analyze(any(), any());
    }

    @Test
    void storesCandidateSelectionWhenDeleteTargetMatchesMultipleSchedules() {
        KakaoLinkService links = mock(KakaoLinkService.class);
        NaturalLanguageProvider provider = mock(NaturalLanguageProvider.class);
        ScheduleRepository schedules = mock(ScheduleRepository.class);
        ChatbotCommandStateStore states = mock(ChatbotCommandStateStore.class);
        User user = new User("user@example.com", "hash");
        ScheduleCommand command = new ScheduleCommand("1.0", ScheduleIntent.DELETE_EVENT, CommandStatus.READY,
                null, "내일", null, null, "회의", List.of(), List.of(), null);
        when(links.linkedUser("kakao-user")).thenReturn(Optional.of(user));
        when(links.protectedExternalUserKey("kakao-user")).thenReturn("protected-key");
        when(provider.analyze(eq("내일 회의 삭제해줘"), any())).thenReturn(command);
        when(schedules.findInRange(eq(user), any(), any())).thenReturn(List.of(
                schedule(user, 11L, "회의", "2026-08-05T01:00:00Z", "2026-08-05T02:00:00Z"),
                schedule(user, 12L, "회의", "2026-08-05T06:00:00Z", "2026-08-05T07:00:00Z")));
        KakaoScheduleAssistantService service = new KakaoScheduleAssistantService(links, provider,
                new KoreanDateRangeResolver(), schedules, states, mock(ChatbotScheduleCreationService.class),
                mock(ChatbotScheduleDeletionService.class), mock(ChatbotScheduleUpdateService.class),
                Clock.fixed(Instant.parse("2026-08-04T05:00:00Z"), ZoneOffset.UTC));

        assertThat(service.reply("kakao-user", "내일 회의 삭제해줘"))
                .contains("어떤 일정을 삭제할까요", "1. 10:00~11:00", "2. 15:00~16:00");
        ArgumentCaptor<ChatbotCommandStateStore.PendingSelection> captor =
                ArgumentCaptor.forClass(ChatbotCommandStateStore.PendingSelection.class);
        verify(states).saveSelection(eq("protected-key"), captor.capture());
        assertThat(captor.getValue().candidates()).extracting(
                ChatbotCommandStateStore.SelectionCandidate::scheduleId).containsExactly(11L, 12L);
    }

    @Test
    void repeatsCandidatesWhenSelectionIsNotSpecificEnough() {
        KakaoLinkService links = mock(KakaoLinkService.class);
        NaturalLanguageProvider provider = mock(NaturalLanguageProvider.class);
        ChatbotCommandStateStore states = mock(ChatbotCommandStateStore.class);
        User user = new User("user@example.com", "hash");
        ChatbotCommandStateStore.PendingSelection selection = new ChatbotCommandStateStore.PendingSelection(
                ChatbotCommandStateStore.SelectionAction.DELETE,
                List.of(candidate(11L, "회의", "2026-08-05T01:00:00Z", "2026-08-05T02:00:00Z"),
                        candidate(12L, "회의", "2026-08-05T06:00:00Z", "2026-08-05T07:00:00Z")),
                null, null, "내일");
        when(links.linkedUser("kakao-user")).thenReturn(Optional.of(user));
        when(links.protectedExternalUserKey("kakao-user")).thenReturn("protected-key");
        when(states.getSelection("protected-key")).thenReturn(Optional.of(selection));
        KakaoScheduleAssistantService service = service(links, provider, states);

        assertThat(service.reply("kakao-user", "그 회의"))
                .contains("어떤 일정을 삭제할까요", "1. 10:00~11:00", "2. 15:00~16:00");
    }

    @Test
    void selectsUniqueAfternoonUpdateCandidate() {
        KakaoLinkService links = mock(KakaoLinkService.class);
        NaturalLanguageProvider provider = mock(NaturalLanguageProvider.class);
        ChatbotCommandStateStore states = mock(ChatbotCommandStateStore.class);
        User user = new User("user@example.com", "hash");
        ChatbotCommandStateStore.PendingSelection selection = new ChatbotCommandStateStore.PendingSelection(
                ChatbotCommandStateStore.SelectionAction.UPDATE,
                List.of(candidate(11L, "회의", "2026-08-05T01:00:00Z", "2026-08-05T02:00:00Z"),
                        candidate(12L, "회의", "2026-08-05T06:00:00Z", "2026-08-05T07:00:00Z")),
                "2026-08-05T08:00:00Z", "2026-08-05T09:00:00Z", "내일");
        when(links.linkedUser("kakao-user")).thenReturn(Optional.of(user));
        when(links.protectedExternalUserKey("kakao-user")).thenReturn("protected-key");
        when(states.getSelection("protected-key")).thenReturn(Optional.of(selection));
        KakaoScheduleAssistantService service = service(links, provider, states);

        assertThat(service.reply("kakao-user", "오후 일정"))
                .contains("회의", "17:00부터 18:00까지", "확인");
        verify(states).saveUpdate(eq("protected-key"), eq(new ChatbotCommandStateStore.PendingUpdate(
                12L, 1L, "회의", "2026-08-05T08:00:00Z", "2026-08-05T09:00:00Z", "내일")));
    }

    private static KakaoScheduleAssistantService service(KakaoLinkService links,
            NaturalLanguageProvider provider, ChatbotCommandStateStore states) {
        return new KakaoScheduleAssistantService(links, provider, new KoreanDateRangeResolver(),
                mock(ScheduleRepository.class), states, mock(ChatbotScheduleCreationService.class),
                mock(ChatbotScheduleDeletionService.class), mock(ChatbotScheduleUpdateService.class),
                Clock.fixed(Instant.parse("2026-08-04T05:00:00Z"), ZoneOffset.UTC));
    }

    private static ChatbotCommandStateStore.SelectionCandidate candidate(Long id, String title,
            String startAt, String endAt) {
        return new ChatbotCommandStateStore.SelectionCandidate(id, 1L, title, startAt, endAt);
    }

    private static Schedule schedule(User user, Long id, String title, String startAt, String endAt) {
        Schedule schedule = new Schedule(user, title, null, Instant.parse(startAt), Instant.parse(endAt));
        ReflectionTestUtils.setField(schedule, "id", id);
        ReflectionTestUtils.setField(schedule, "version", 1L);
        return schedule;
    }

    private static ScheduleCommand createCommand() {
        return new ScheduleCommand("1.0", ScheduleIntent.CREATE_EVENT, CommandStatus.READY,
                "팀 미팅", "내일", "15:00", "16:00", null, List.of(), List.of(), null);
    }
}
