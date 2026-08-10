package com.caltalk.backend.ai;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleCommandValidatorTests {
    private final ScheduleCommandValidator validator = new ScheduleCommandValidator();

    @Test
    void acceptsCompleteCreateCommand() {
        ScheduleCommand result = validator.validate(new ScheduleCommand(null, ScheduleIntent.CREATE_EVENT,
                CommandStatus.READY, "팀 미팅", "내일", "15:00", "16:00", null, List.of(), List.of(), null));

        assertThat(result.status()).isEqualTo(CommandStatus.READY);
        assertThat(result.schemaVersion()).isEqualTo("1.0");
        assertThat(result.missingFields()).isEmpty();
    }

    @Test
    void rejectsCreateCommandMissingTimeEvenWhenModelSaysReady() {
        ScheduleCommand result = validator.validate(new ScheduleCommand("1.0", ScheduleIntent.CREATE_EVENT,
                CommandStatus.READY, "팀 미팅", "내일", null, "16:00", null, List.of(), List.of(), null));

        assertThat(result.status()).isEqualTo(CommandStatus.NEEDS_CLARIFICATION);
        assertThat(result.missingFields()).containsExactly("startTime");
    }

    @Test
    void requiresTargetAndNewTimeForAmbiguousUpdate() {
        ScheduleCommand result = validator.validate(new ScheduleCommand("1.0", ScheduleIntent.UPDATE_EVENT,
                CommandStatus.READY, null, null, null, null, null, List.of(), List.of(), null));

        assertThat(result.status()).isEqualTo(CommandStatus.NEEDS_CLARIFICATION);
        assertThat(result.missingFields()).containsExactly("targetEvent", "date", "startTime", "endTime");
        assertThat(result.userFacingQuestion()).isEqualTo("어느 날짜의 어떤 일정을 몇 시부터 몇 시까지로 변경할까요?");
    }
}
