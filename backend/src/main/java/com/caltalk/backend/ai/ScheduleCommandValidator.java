package com.caltalk.backend.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ScheduleCommandValidator {

    public ScheduleCommand validate(ScheduleCommand command) {
        if (command == null || command.intent() == null || command.status() == null) {
            return clarification(ScheduleIntent.UNKNOWN, List.of("intent"), "일정에 대해 무엇을 도와드릴까요?");
        }

        List<String> missing = new ArrayList<>(command.missingFields());
        switch (command.intent()) {
            case CREATE_EVENT -> {
                removeWhenPresent(missing, "title", command.title());
                removeWhenPresent(missing, "date", command.dateExpression());
                removeWhenPresent(missing, "startTime", command.startTime());
                removeWhenPresent(missing, "endTime", command.endTime());
                require(missing, "title", command.title());
                require(missing, "date", command.dateExpression());
                require(missing, "startTime", command.startTime());
                require(missing, "endTime", command.endTime());
            }
            case SEARCH_EVENTS, FREE_TIME_QUERY -> require(missing, "date", command.dateExpression());
            case UPDATE_EVENT -> {
                require(missing, "targetEvent", command.targetExpression());
                require(missing, "date", command.dateExpression());
                require(missing, "startTime", command.startTime());
                require(missing, "endTime", command.endTime());
            }
            case DELETE_EVENT -> {
                require(missing, "targetEvent", command.targetExpression());
                require(missing, "date", command.dateExpression());
            }
            case UNKNOWN -> addOnce(missing, "intent");
        }

        if (!missing.isEmpty()) {
            String question = blank(command.userFacingQuestion())
                    ? defaultQuestion(command.intent())
                    : command.userFacingQuestion();
            return new ScheduleCommand("1.0", command.intent(), CommandStatus.NEEDS_CLARIFICATION,
                    command.title(), command.dateExpression(), command.startTime(), command.endTime(), command.targetExpression(),
                    missing, command.ambiguities(), question);
        }

        return new ScheduleCommand("1.0", command.intent(), command.status(), command.title(),
                command.dateExpression(), command.startTime(), command.endTime(), command.targetExpression(), List.of(),
                command.ambiguities(), command.userFacingQuestion());
    }

    private static ScheduleCommand clarification(ScheduleIntent intent, List<String> missing, String question) {
        return new ScheduleCommand("1.0", intent, CommandStatus.NEEDS_CLARIFICATION, null, null, null,
                null, null, missing, List.of(), question);
    }

    private static void require(List<String> missing, String field, String value) {
        if (blank(value)) addOnce(missing, field);
    }

    private static void addOnce(List<String> values, String value) {
        if (!values.contains(value)) values.add(value);
    }

    private static void removeWhenPresent(List<String> values, String field, String value) {
        if (!blank(value)) values.removeIf(field::equals);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String defaultQuestion(ScheduleIntent intent) {
        return switch (intent) {
            case CREATE_EVENT -> "일정 제목과 날짜, 시작 시간과 종료 시간을 알려주세요.";
            case SEARCH_EVENTS -> "어느 날짜의 일정을 확인할까요?";
            case FREE_TIME_QUERY -> "어느 날짜의 빈 시간을 확인할까요?";
            case UPDATE_EVENT -> "어느 날짜의 어떤 일정을 몇 시부터 몇 시까지로 변경할까요?";
            case DELETE_EVENT -> "어떤 일정을 삭제할까요?";
            case UNKNOWN -> "일정에 대해 무엇을 도와드릴까요?";
        };
    }
}
