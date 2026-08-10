package com.caltalk.backend.ai;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoutingNaturalLanguageProviderTests {
    private final OllamaNaturalLanguageProvider ollama = mock(OllamaNaturalLanguageProvider.class);
    private final OpenAiNaturalLanguageProvider openAi = mock(OpenAiNaturalLanguageProvider.class);
    private final OpenAiFallbackBudget budget = mock(OpenAiFallbackBudget.class);
    private final AnalysisContext context = new AnalysisContext(
            ZonedDateTime.of(2026, 8, 4, 17, 0, 0, 0, ZoneId.of("Asia/Seoul")));
    private RoutingNaturalLanguageProvider provider;

    @BeforeEach
    void setUp() {
        provider = new RoutingNaturalLanguageProvider(ollama, openAi, budget);
        when(ollama.available()).thenReturn(true);
    }

    @Test
    void keepsReadyLocalResultWithoutSpendingFallbackBudget() {
        ScheduleCommand local = command(ScheduleIntent.SEARCH_EVENTS, CommandStatus.READY);
        when(ollama.analyze("tomorrow", context)).thenReturn(local);

        assertThat(provider.analyze("tomorrow", context)).isSameAs(local);
        verify(openAi, never()).analyze("tomorrow", context);
        verify(budget, never()).tryAcquire();
    }

    @Test
    void keepsClarificationLocalWithoutSpendingFallbackBudget() {
        ScheduleCommand local = command(ScheduleIntent.CREATE_EVENT, CommandStatus.NEEDS_CLARIFICATION);
        when(ollama.analyze("meeting", context)).thenReturn(local);

        assertThat(provider.analyze("meeting", context)).isSameAs(local);
        verify(openAi, never()).analyze("meeting", context);
        verify(budget, never()).tryAcquire();
    }

    @Test
    void usesOpenAiForUnknownLocalResultWhenAvailableAndWithinBudget() {
        ScheduleCommand local = command(ScheduleIntent.UNKNOWN, CommandStatus.UNSUPPORTED);
        ScheduleCommand fallback = command(ScheduleIntent.FREE_TIME_QUERY, CommandStatus.READY);
        when(ollama.analyze("complex request", context)).thenReturn(local);
        when(openAi.available()).thenReturn(true);
        when(budget.tryAcquire()).thenReturn(true);
        when(openAi.analyze("complex request", context)).thenReturn(fallback);

        assertThat(provider.analyze("complex request", context)).isSameAs(fallback);
    }

    @Test
    void preservesLocalFailureWhenFallbackBudgetIsUnavailable() {
        NaturalLanguageProviderException failure = new NaturalLanguageProviderException("local failed");
        when(ollama.analyze("request", context)).thenThrow(failure);
        when(openAi.available()).thenReturn(true);
        when(budget.tryAcquire()).thenReturn(false);

        assertThatThrownBy(() -> provider.analyze("request", context)).isSameAs(failure);
        verify(openAi, never()).analyze("request", context);
    }

    @Test
    void usesOpenAiImmediatelyWhenOllamaIsDisabled() {
        ScheduleCommand cloud = command(ScheduleIntent.SEARCH_EVENTS, CommandStatus.READY);
        when(ollama.available()).thenReturn(false);
        when(openAi.available()).thenReturn(true);
        when(budget.tryAcquire()).thenReturn(true);
        when(openAi.analyze("request", context)).thenReturn(cloud);

        assertThat(provider.analyze("request", context)).isSameAs(cloud);
        verify(ollama, never()).analyze("request", context);
    }

    private static ScheduleCommand command(ScheduleIntent intent, CommandStatus status) {
        return new ScheduleCommand("1.0", intent, status, null, null, null, null, null,
                List.of(), List.of(), null);
    }
}
