package com.caltalk.backend.chatbot;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalTime;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.caltalk.backend.ai.AnalysisContext;
import com.caltalk.backend.ai.CommandStatus;
import com.caltalk.backend.ai.KoreanDateRangeResolver;
import com.caltalk.backend.ai.NaturalLanguageProvider;
import com.caltalk.backend.ai.NaturalLanguageProviderException;
import com.caltalk.backend.ai.ResolvedDateRange;
import com.caltalk.backend.ai.ScheduleCommand;
import com.caltalk.backend.kakao.KakaoLinkService;
import com.caltalk.backend.schedule.Schedule;
import com.caltalk.backend.schedule.ScheduleRepository;
import com.caltalk.backend.user.User;

@Service
public class KakaoScheduleAssistantService {
    private static final DateTimeFormatter TWELVE_HOUR_FORMAT = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN);
    private static final DateTimeFormatter TWENTY_FOUR_HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final KakaoLinkService linkService;
    private final NaturalLanguageProvider languageProvider;
    private final KoreanDateRangeResolver dateResolver;
    private final ScheduleRepository schedules;
    private final ChatbotCommandStateStore stateStore;
    private final ChatbotScheduleCreationService creationService;
    private final ChatbotScheduleDeletionService deletionService;
    private final ChatbotScheduleUpdateService updateService;
    private final Clock clock;

    @Autowired
    public KakaoScheduleAssistantService(KakaoLinkService linkService, NaturalLanguageProvider languageProvider,
            KoreanDateRangeResolver dateResolver, ScheduleRepository schedules,
            ChatbotCommandStateStore stateStore, ChatbotScheduleCreationService creationService,
            ChatbotScheduleDeletionService deletionService, ChatbotScheduleUpdateService updateService) {
        this(linkService, languageProvider, dateResolver, schedules, stateStore, creationService,
                deletionService, updateService, Clock.systemUTC());
    }

    KakaoScheduleAssistantService(KakaoLinkService linkService, NaturalLanguageProvider languageProvider,
            KoreanDateRangeResolver dateResolver, ScheduleRepository schedules,
            ChatbotCommandStateStore stateStore, ChatbotScheduleCreationService creationService,
            ChatbotScheduleDeletionService deletionService, ChatbotScheduleUpdateService updateService, Clock clock) {
        this.linkService = linkService;
        this.languageProvider = languageProvider;
        this.dateResolver = dateResolver;
        this.schedules = schedules;
        this.stateStore = stateStore;
        this.creationService = creationService;
        this.deletionService = deletionService;
        this.updateService = updateService;
        this.clock = clock;
    }

    public String reply(String externalUserId, String utterance) {
        User user = linkService.linkedUser(externalUserId).orElse(null);
        if (user == null) return "먼저 CalTalk 계정을 연결해 주세요.";
        String formatted = applyReplyStyle(user,
                applyReplyLayout(user, replyForLinkedUser(user, externalUserId, utterance)));
        return applyEmojiLevel(user, formatted);
    }

    private String replyForLinkedUser(User user, String externalUserId, String utterance) {
        ZoneId zone = safeZone(user.getTimezone());
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(zone);
        String stateKey = linkService.protectedExternalUserKey(externalUserId);
        try {
            ChatbotCommandStateStore.PendingSelection pendingSelection = stateStore.getSelection(stateKey).orElse(null);
            if (isCancel(utterance) && (stateStore.get(stateKey).isPresent()
                    || stateStore.getDelete(stateKey).isPresent() || stateStore.getUpdate(stateKey).isPresent()
                    || pendingSelection != null)) {
                stateStore.delete(stateKey);
                stateStore.deleteDelete(stateKey);
                stateStore.deleteUpdate(stateKey);
                stateStore.deleteSelection(stateKey);
                return "일정 요청을 취소했어요.";
            }
            if (pendingSelection != null) {
                if (isConfirmation(utterance)) return selectionPrompt(pendingSelection, zone);
                Integer selectedIndex = resolveSelection(utterance, pendingSelection, zone);
                if (selectedIndex == null) return selectionPrompt(pendingSelection, zone);
                return selectCandidate(stateKey, pendingSelection, selectedIndex, zone, user);
            }
            if (isConfirmation(utterance)) {
                ScheduleCommand pending = stateStore.get(stateKey).orElse(null);
                if (pending != null) {
                    stateStore.delete(stateKey);
                    return creationService.create(user, zone, now, pending).message();
                }
                ChatbotCommandStateStore.PendingDelete pendingDelete = stateStore.getDelete(stateKey).orElse(null);
                if (pendingDelete != null) {
                    stateStore.deleteDelete(stateKey);
                    return deletionService.delete(user, pendingDelete);
                }
                ChatbotCommandStateStore.PendingUpdate pendingUpdate = stateStore.getUpdate(stateKey).orElse(null);
                if (pendingUpdate != null) {
                    stateStore.deleteUpdate(stateKey);
                    return updateService.update(user, pendingUpdate);
                }
                return "확인할 일정 요청이 없어요.";
            }
            ScheduleCommand draft = stateStore.get(stateKey).orElse(null);
            String analysisInput = draft != null && draft.status() == CommandStatus.NEEDS_CLARIFICATION
                    ? mergePrompt(draft, utterance) : utterance;
            ScheduleCommand command = languageProvider.analyze(analysisInput, new AnalysisContext(now));
            command = applyDefaultDuration(user, command);
            command = applyDefaultQueryRange(user, command);
            if (command.status() == CommandStatus.NEEDS_CLARIFICATION) {
                if (command.intent() == com.caltalk.backend.ai.ScheduleIntent.CREATE_EVENT) {
                    stateStore.save(stateKey, command);
                }
                return command.userFacingQuestion();
            }
            return switch (command.intent()) {
                case SEARCH_EVENTS -> search(user, zone, now, command);
                case FREE_TIME_QUERY -> freeTime(user, zone, now, command);
                case CREATE_EVENT -> user.isChatConfirmCreate()
                        ? prepareCreation(stateKey, command)
                        : creationService.create(user, zone, now, command).message();
                case DELETE_EVENT -> prepareDeletion(stateKey, user, zone, now, command);
                case UPDATE_EVENT -> prepareUpdate(stateKey, user, zone, now, command);
                case UNKNOWN -> "일정 조회나 등록, 변경, 삭제에 대해 말씀해 주세요.";
            };
        } catch (NaturalLanguageProviderException | IllegalStateException exception) {
            return "일정 문장을 해석하는 데 잠시 문제가 생겼어요. 잠시 후 다시 말씀해 주세요.";
        }
    }

    private String prepareCreation(String stateKey, ScheduleCommand command) {
        stateStore.deleteDelete(stateKey);
        stateStore.deleteUpdate(stateKey);
        stateStore.deleteSelection(stateKey);
        stateStore.save(stateKey, command);
        return "%s\n%s %s부터 %s까지\n등록하려면 '확인', 취소하려면 '취소'라고 답해 주세요.".formatted(
                command.title(), command.dateExpression(), command.startTime(), command.endTime());
    }

    private static ScheduleCommand applyDefaultDuration(User user, ScheduleCommand command) {
        if (command.intent() != com.caltalk.backend.ai.ScheduleIntent.CREATE_EVENT
                || command.startTime() == null || command.startTime().isBlank()
                || (command.endTime() != null && !command.endTime().isBlank())) {
            return command;
        }
        LocalTime start = parseClockTime(command.startTime());
        if (start == null) return command;
        String endTime = start.plusMinutes(user.getChatDefaultDurationMinutes())
                .format(DateTimeFormatter.ofPattern("HH:mm"));
        List<String> missing = command.missingFields().stream()
                .filter(field -> !"endTime".equals(field))
                .toList();
        CommandStatus status = missing.isEmpty() ? CommandStatus.READY : command.status();
        return new ScheduleCommand(command.schemaVersion(), command.intent(), status, command.title(),
                command.dateExpression(), command.startTime(), endTime, command.targetExpression(),
                missing, command.ambiguities(), command.userFacingQuestion());
    }

    private static ScheduleCommand applyDefaultQueryRange(User user, ScheduleCommand command) {
        if (command.intent() != com.caltalk.backend.ai.ScheduleIntent.SEARCH_EVENTS
                || (command.dateExpression() != null && !command.dateExpression().isBlank())) {
            return command;
        }
        String expression = switch (user.getChatDefaultQueryRange()) {
            case "THREE_DAYS" -> "앞으로 3일";
            case "THIS_WEEK" -> "이번 주";
            case "NEXT_FIVE" -> "앞으로 365일";
            default -> "오늘";
        };
        List<String> missing = command.missingFields().stream()
                .filter(field -> !"date".equals(field))
                .toList();
        CommandStatus status = missing.isEmpty() ? CommandStatus.READY : command.status();
        return new ScheduleCommand(command.schemaVersion(), command.intent(), status, command.title(),
                expression, command.startTime(), command.endTime(), command.targetExpression(),
                missing, command.ambiguities(), command.userFacingQuestion());
    }

    private String prepareDeletion(String stateKey, User user, ZoneId zone, ZonedDateTime now,
            ScheduleCommand command) {
        ResolvedDateRange range = dateResolver.resolve(command.dateExpression(), now).orElse(null);
        if (range == null) return "삭제할 일정의 날짜를 다시 알려주세요.";
        String target = command.targetExpression() == null ? "" : command.targetExpression().trim();
        List<Schedule> matches = schedules.findInRange(user, range.from(), range.to()).stream()
                .filter(schedule -> schedule.getTitle().equalsIgnoreCase(target)
                        || schedule.getTitle().toLowerCase().contains(target.toLowerCase()))
                .toList();
        if (matches.isEmpty()) return "해당 날짜에 '%s' 일정을 찾지 못했어요.".formatted(target);
        if (matches.size() > 1) {
            ChatbotCommandStateStore.PendingSelection selection = pendingSelection(
                    ChatbotCommandStateStore.SelectionAction.DELETE, matches, null, null,
                    command.dateExpression());
            clearPendingCommands(stateKey);
            stateStore.saveSelection(stateKey, selection);
            return selectionPrompt(selection, zone);
        }
        Schedule targetSchedule = matches.getFirst();
        stateStore.delete(stateKey);
        stateStore.deleteUpdate(stateKey);
        stateStore.deleteSelection(stateKey);
        stateStore.saveDelete(stateKey, new ChatbotCommandStateStore.PendingDelete(
                targetSchedule.getId(), targetSchedule.getVersion(), targetSchedule.getTitle(),
                command.dateExpression()));
        return "'%s' 일정을 삭제할까요?\n삭제하려면 '확인', 취소하려면 '취소'라고 답해 주세요.".formatted(
                targetSchedule.getTitle());
    }

    private String prepareUpdate(String stateKey, User user, ZoneId zone, ZonedDateTime now,
            ScheduleCommand command) {
        ResolvedDateRange range = dateResolver.resolve(command.dateExpression(), now).orElse(null);
        LocalTime start = parseClockTime(command.startTime());
        LocalTime end = parseClockTime(command.endTime());
        if (range == null || start == null || end == null || !end.isAfter(start)) {
            return "변경할 날짜와 시작·종료 시간을 다시 알려주세요.";
        }
        String target = command.targetExpression() == null ? "" : command.targetExpression().trim();
        List<Schedule> matches = schedules.findInRange(user, range.from(), range.to()).stream()
                .filter(schedule -> schedule.getTitle().equalsIgnoreCase(target)
                        || schedule.getTitle().toLowerCase().contains(target.toLowerCase()))
                .toList();
        if (matches.isEmpty()) return "해당 날짜에 '%s' 일정을 찾지 못했어요.".formatted(target);
        ZonedDateTime newStart = range.fromDate().atTime(start).atZone(zone);
        ZonedDateTime newEnd = range.fromDate().atTime(end).atZone(zone);
        if (matches.size() > 1) {
            ChatbotCommandStateStore.PendingSelection selection = pendingSelection(
                    ChatbotCommandStateStore.SelectionAction.UPDATE, matches,
                    newStart.toInstant().toString(), newEnd.toInstant().toString(), command.dateExpression());
            clearPendingCommands(stateKey);
            stateStore.saveSelection(stateKey, selection);
            return selectionPrompt(selection, zone);
        }
        Schedule schedule = matches.getFirst();
        stateStore.delete(stateKey);
        stateStore.deleteDelete(stateKey);
        stateStore.deleteSelection(stateKey);
        ChatbotCommandStateStore.PendingUpdate pendingUpdate = new ChatbotCommandStateStore.PendingUpdate(
                schedule.getId(), schedule.getVersion(), schedule.getTitle(),
                newStart.toInstant().toString(), newEnd.toInstant().toString(), command.dateExpression());
        if (!user.isChatConfirmUpdate()) {
            return updateService.update(user, pendingUpdate);
        }
        stateStore.saveUpdate(stateKey, pendingUpdate);
        return "'%s' 일정을 %s부터 %s까지로 변경할까요?\n변경하려면 '확인', 취소하려면 '취소'라고 답해 주세요.".formatted(
                schedule.getTitle(), command.startTime(), command.endTime());
    }

    private ChatbotCommandStateStore.PendingSelection pendingSelection(
            ChatbotCommandStateStore.SelectionAction action, List<Schedule> matches,
            String newStartAt, String newEndAt, String dateLabel) {
        List<ChatbotCommandStateStore.SelectionCandidate> candidates = matches.stream()
                .limit(5)
                .map(schedule -> new ChatbotCommandStateStore.SelectionCandidate(
                        schedule.getId(), schedule.getVersion(), schedule.getTitle(),
                        schedule.getStartAt().toString(), schedule.getEndAt().toString()))
                .toList();
        return new ChatbotCommandStateStore.PendingSelection(action, candidates,
                newStartAt, newEndAt, dateLabel);
    }

    private String selectCandidate(String stateKey, ChatbotCommandStateStore.PendingSelection selection,
            int index, ZoneId zone, User user) {
        ChatbotCommandStateStore.SelectionCandidate candidate = selection.candidates().get(index);
        stateStore.deleteSelection(stateKey);
        if (selection.action() == ChatbotCommandStateStore.SelectionAction.DELETE) {
            stateStore.saveDelete(stateKey, new ChatbotCommandStateStore.PendingDelete(
                    candidate.scheduleId(), candidate.version(), candidate.title(), selection.dateLabel()));
            return "'%s' 일정을 삭제할까요?\n삭제하려면 '확인', 취소하려면 '취소'라고 답해 주세요."
                    .formatted(candidate.title());
        }
        ChatbotCommandStateStore.PendingUpdate pendingUpdate = new ChatbotCommandStateStore.PendingUpdate(
                candidate.scheduleId(), candidate.version(), candidate.title(),
                selection.newStartAt(), selection.newEndAt(), selection.dateLabel());
        if (!user.isChatConfirmUpdate()) {
            return updateService.update(user, pendingUpdate);
        }
        stateStore.saveUpdate(stateKey, pendingUpdate);
        String start = Instant.parse(selection.newStartAt()).atZone(zone)
                .format(DateTimeFormatter.ofPattern("HH:mm"));
        String end = Instant.parse(selection.newEndAt()).atZone(zone)
                .format(DateTimeFormatter.ofPattern("HH:mm"));
        return "'%s' 일정을 %s부터 %s까지로 변경할까요?\n변경하려면 '확인', 취소하려면 '취소'라고 답해 주세요."
                .formatted(candidate.title(), start, end);
    }

    private static Integer resolveSelection(String utterance,
            ChatbotCommandStateStore.PendingSelection selection, ZoneId zone) {
        String value = utterance == null ? "" : utterance.trim().toLowerCase(Locale.ROOT);
        String compact = value.replaceAll("[\\s.!?]", "");
        String[] ordinals = {"첫번째", "두번째", "세번째", "네번째", "다섯번째"};
        for (int i = 0; i < selection.candidates().size(); i++) {
            if (compact.equals(String.valueOf(i + 1)) || compact.equals((i + 1) + "번")
                    || compact.equals(ordinals[i])) return i;
        }
        List<Integer> matches = new ArrayList<>();
        for (int i = 0; i < selection.candidates().size(); i++) {
            ChatbotCommandStateStore.SelectionCandidate candidate = selection.candidates().get(i);
            ZonedDateTime start = Instant.parse(candidate.startAt()).atZone(zone);
            boolean titleMatches = !candidate.title().isBlank()
                    && value.contains(candidate.title().toLowerCase(Locale.ROOT));
            boolean timeMatches = value.contains(start.format(DateTimeFormatter.ofPattern("HH:mm")));
            boolean periodMatches = (value.contains("오전") && start.getHour() < 12)
                    || (value.contains("오후") && start.getHour() >= 12);
            if (titleMatches || timeMatches || periodMatches) matches.add(i);
        }
        return matches.size() == 1 ? matches.getFirst() : null;
    }

    private static String selectionPrompt(ChatbotCommandStateStore.PendingSelection selection, ZoneId zone) {
        String action = selection.action() == ChatbotCommandStateStore.SelectionAction.DELETE ? "삭제" : "변경";
        StringBuilder reply = new StringBuilder("어떤 일정을 ").append(action).append("할까요?");
        for (int i = 0; i < selection.candidates().size(); i++) {
            ChatbotCommandStateStore.SelectionCandidate candidate = selection.candidates().get(i);
            ZonedDateTime start = Instant.parse(candidate.startAt()).atZone(zone);
            ZonedDateTime end = Instant.parse(candidate.endAt()).atZone(zone);
            reply.append("\n").append(i + 1).append(". ")
                    .append(start.format(DateTimeFormatter.ofPattern("HH:mm"))).append("~")
                    .append(end.format(DateTimeFormatter.ofPattern("HH:mm"))).append("  ")
                    .append(candidate.title());
        }
        return reply.append("\n번호나 시간을 말씀해 주세요. 취소하려면 '취소'라고 답해 주세요.").toString();
    }

    private void clearPendingCommands(String stateKey) {
        stateStore.delete(stateKey);
        stateStore.deleteDelete(stateKey);
        stateStore.deleteUpdate(stateKey);
        stateStore.deleteSelection(stateKey);
    }

    private static LocalTime parseClockTime(String value) {
        if (value == null) return null;
        try {
            return LocalTime.parse(value, DateTimeFormatter.ofPattern("H:mm"));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String mergePrompt(ScheduleCommand draft, String utterance) {
        return """
                이전 일정 요청:
                intent=%s, title=%s, date=%s, startTime=%s, endTime=%s
                사용자의 새 답변: %s
                이전 값과 새 답변을 합쳐 하나의 일정 명령으로 반환하세요.
                """.formatted(draft.intent(), draft.title(), draft.dateExpression(), draft.startTime(),
                draft.endTime(), utterance);
    }

    private static boolean isConfirmation(String utterance) {
        String value = utterance == null ? "" : utterance.replaceAll("[\\s.!?]", "");
        return value.equals("확인") || value.equals("네") || value.equals("응") || value.equals("등록해줘");
    }

    private static boolean isCancel(String utterance) {
        String value = utterance == null ? "" : utterance.replaceAll("[\\s.!?]", "");
        return value.equals("취소") || value.equals("아니") || value.equals("아니요");
    }

    private String search(User user, ZoneId zone, ZonedDateTime now, ScheduleCommand command) {
        ResolvedDateRange range = dateResolver.resolve(command.dateExpression(), now).orElse(null);
        if (range == null) return "어느 날짜의 일정을 확인할까요? 예: 내일 일정 알려줘";
        List<Schedule> found = schedules.findInRange(user, range.from(), range.to());
        String label = range.fromDate().equals(range.toDateExclusive().minusDays(1))
                ? range.fromDate().getMonthValue() + "월 " + range.fromDate().getDayOfMonth() + "일"
                : range.fromDate() + "부터 " + range.toDateExclusive().minusDays(1) + "까지";
        if (found.isEmpty()) return label + "에는 등록된 일정이 없어요.";
        String density = user.getChatReplyDensity();
        StringBuilder reply = new StringBuilder();
        if ("ESSENTIAL".equals(density)) {
            reply.append(label).append(" · ").append(found.size()).append("개");
        } else {
            reply.append(label).append(" 일정이에요.");
        }
        int limit = Math.min(found.size(), 5);
        for (int i = 0; i < limit; i++) {
            Schedule schedule = found.get(i);
            reply.append("\n• ").append(schedule.getStartAt().atZone(zone).format(timeFormatter(user)));
            if (!"ESSENTIAL".equals(density)) {
                reply.append("~").append(schedule.getEndAt().atZone(zone).format(timeFormatter(user)));
            }
            reply.append("  ").append(schedule.getTitle());
            if ("DETAILED".equals(density)
                    && schedule.getLocation() != null && !schedule.getLocation().isBlank()) {
                reply.append("\n  장소 · ").append(schedule.getLocation());
            }
        }
        if (found.size() > limit) reply.append("\n외 ").append(found.size() - limit).append("개의 일정이 더 있어요.");
        return reply.toString();
    }

    private static DateTimeFormatter timeFormatter(User user) {
        return "TWENTY_FOUR_HOUR".equals(user.getChatTimeFormat())
                ? TWENTY_FOUR_HOUR_FORMAT : TWELVE_HOUR_FORMAT;
    }

    private static String applyReplyStyle(User user, String message) {
        if (message == null || message.isBlank()) return message;
        return switch (user.getChatReplyStyle()) {
            case "CONCISE" -> message
                    .replace("일정이에요.", "일정")
                    .replace("등록했어요.", "등록 완료")
                    .replace("변경했어요.", "변경 완료")
                    .replace("삭제했어요.", "삭제 완료");
            case "BUSINESS" -> message
                    .replace("했어요.", "했습니다.")
                    .replace("없어요.", "없습니다.")
                    .replace("주세요.", "주시기 바랍니다.")
                    .replace("할까요?", "하시겠습니까?");
            case "ASSISTANT" -> message + (message.contains("완료") || message.contains("했어요.")
                    ? "\n필요한 일정이 더 있으면 이어서 말씀해 주세요." : "");
            case "FRIENDLY" -> message.replace("말씀해 주세요.", "편하게 말씀해 주세요.");
            default -> message;
        };
    }

    private static String applyReplyLayout(User user, String message) {
        if (message == null || message.isBlank() || "BALANCED".equals(user.getChatReplyLayout())) return message;
        String[] lines = message.split("\\R");
        if ("COMPACT".equals(user.getChatReplyLayout())) {
            long scheduleLineCount = java.util.Arrays.stream(lines)
                    .map(String::trim)
                    .filter(line -> line.startsWith("•"))
                    .count();
            if (scheduleLineCount > 0) {
                StringBuilder compactSchedule = new StringBuilder(
                        lines[0].trim().replace(" 일정이에요.", " · " + scheduleLineCount + "개"));
                for (int i = 1; i < lines.length; i++) {
                    String value = lines[i].trim();
                    if (!value.isEmpty()) compactSchedule.append("\n").append(value);
                }
                return compactSchedule.toString();
            }
            StringBuilder content = new StringBuilder();
            String instruction = null;
            for (String line : lines) {
                String value = line.trim();
                if (value.isEmpty()) continue;
                if (isInstructionLine(value)) {
                    instruction = value;
                    continue;
                }
                value = value.replaceFirst("^[•]\\s*", "");
                if (!content.isEmpty()) content.append(" · ");
                content.append(value);
            }
            return instruction == null ? content.toString() : content + "\n" + instruction;
        }
        StringBuilder sectioned = new StringBuilder();
        int scheduleNumber = 0;
        for (String line : lines) {
            String value = line.trim();
            if (value.isEmpty()) continue;
            if (value.startsWith("•")) {
                scheduleNumber += 1;
                String item = value.replaceFirst("^[•]\\s*", "");
                String[] parts = item.split("\\s{2,}", 2);
                sectioned.append("\n• 일정 ").append(scheduleNumber);
                if (parts.length == 2) {
                    sectioned.append("\n  ").append(parts[1]);
                    sectioned.append("\n  시간 · ").append(parts[0]);
                } else {
                    sectioned.append("\n  ").append(item);
                }
                continue;
            }
            if (!sectioned.isEmpty() && isInstructionLine(value)) {
                sectioned.append("\n");
            }
            if (scheduleNumber > 0 && value.startsWith("장소 ·")) {
                sectioned.append("\n  ").append(value);
                continue;
            }
            if (!sectioned.isEmpty()) sectioned.append("\n");
            sectioned.append(value);
        }
        return sectioned.toString();
    }

    private static boolean isInstructionLine(String value) {
        return value.contains("라고 답해 주세요") || value.startsWith("번호나 시간을 말씀해 주세요");
    }

    private static String applyEmojiLevel(User user, String message) {
        if (message == null || message.isBlank() || "NONE".equals(user.getChatEmojiLevel())) return message;

        String icon = replyIcon(message);
        String decorated = icon + " " + message;
        if (!"BALANCED".equals(user.getChatEmojiLevel())) return decorated;

        return decorated
                .replace("시간 · ", "🕒 시간 · ")
                .replace("장소 · ", "📍 장소 · ");
    }

    private static String replyIcon(String message) {
        if (message.contains("삭제")) return "🗑️";
        if (message.contains("취소했") || message.contains("등록했") || message.contains("변경했")
                || message.contains("저장했") || message.contains("완료")) return "✅";
        if (message.contains("충돌") || message.contains("확인해 주세요")) return "⚠️";
        if (message.contains("말씀해 주세요") || message.contains("할까요?")) return "💬";
        return "🗓️";
    }

    private String freeTime(User user, ZoneId zone, ZonedDateTime now, ScheduleCommand command) {
        ResolvedDateRange range = dateResolver.resolve(command.dateExpression(), now).orElse(null);
        if (range == null || !range.toDateExclusive().equals(range.fromDate().plusDays(1))) {
            return "빈 시간을 확인할 날짜를 하루 단위로 알려주세요.";
        }
        Instant windowStart = range.fromDate().atTime(9, 0).atZone(zone).toInstant();
        Instant windowEnd = range.fromDate().atTime(18, 0).atZone(zone).toInstant();
        List<Schedule> found = schedules.findInRange(user, windowStart, windowEnd);
        List<TimeGap> gaps = new ArrayList<>();
        Instant cursor = windowStart;
        for (Schedule schedule : found) {
            Instant busyStart = schedule.getStartAt().isBefore(windowStart) ? windowStart : schedule.getStartAt();
            Instant busyEnd = schedule.getEndAt().isAfter(windowEnd) ? windowEnd : schedule.getEndAt();
            if (busyStart.isAfter(cursor) && Duration.between(cursor, busyStart).toMinutes() >= 30) {
                gaps.add(new TimeGap(cursor, busyStart));
            }
            if (busyEnd.isAfter(cursor)) cursor = busyEnd;
        }
        if (cursor.isBefore(windowEnd) && Duration.between(cursor, windowEnd).toMinutes() >= 30) {
            gaps.add(new TimeGap(cursor, windowEnd));
        }
        String label = range.fromDate().getMonthValue() + "월 " + range.fromDate().getDayOfMonth() + "일";
        if (gaps.isEmpty()) return label + "은 09:00부터 18:00까지 빈 시간이 없어요.";
        StringBuilder reply = new StringBuilder(label).append(" 빈 시간이에요. (09:00~18:00 기준)");
        gaps.stream().limit(5).forEach(gap -> reply.append("\n• ")
                .append(gap.from().atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm")))
                .append("~").append(gap.to().atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))));
        return reply.toString();
    }

    private record TimeGap(Instant from, Instant to) {
    }

    private static ZoneId safeZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (RuntimeException ignored) {
            return ZoneId.of("Asia/Seoul");
        }
    }
}
