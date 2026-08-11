package com.caltalk.backend.chatbot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class KakaoSkillResponseFactory {
    private static final Pattern SELECTION_LINE = Pattern.compile("(?m)^(\\d{1,2})\\.\\s+(.+)$");
    private KakaoSkillResponseFactory() {
    }

    static Map<String, Object> response(String message) {
        return Map.of("version", "2.0", "template", template(message));
    }

    static Map<String, Object> template(String message) {
        String safe = message == null || message.isBlank() ? "요청을 처리하지 못했어요." : message.trim();
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("outputs", List.of(output(safe)));
        List<Map<String, String>> quickReplies = quickReplies(safe);
        if (!quickReplies.isEmpty()) template.put("quickReplies", quickReplies);
        return template;
    }

    private static Map<String, Object> output(String message) {
        if (isSelectionPrompt(message)) {
            int lineBreak = message.indexOf('\n');
            String description = lineBreak > 0 ? message.substring(lineBreak + 1) : message;
            return textCard("일정을 선택해 주세요", clamp(description, 340), List.of());
        }
        if (message.contains("등록하려면 '확인'")) {
            return textCard("일정을 등록할까요?", beforeInstruction(message), confirmButtons("등록하기"));
        }
        if (message.contains("변경하려면 '확인'")) {
            return textCard("일정을 변경할까요?", beforeInstruction(message), confirmButtons("변경하기"));
        }
        if (message.contains("삭제하려면 '확인'")) {
            return textCard("일정을 삭제할까요?", beforeInstruction(message), confirmButtons("삭제하기"));
        }
        if (isCompleted(message)) {
            return textCard(completionTitle(message), clamp(message, 400), List.of());
        }
        if (message.contains("\n• ")) {
            int lineBreak = message.indexOf('\n');
            String title = lineBreak > 0 ? message.substring(0, lineBreak) : "일정 조회";
            String description = lineBreak > 0 ? message.substring(lineBreak + 1) : message;
            return textCard(clamp(title, 50), clamp(description, 340), List.of());
        }
        return Map.of("simpleText", Map.of("text", clamp(message, 1_000)));
    }

    private static Map<String, Object> textCard(String title, String description,
            List<Map<String, String>> buttons) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("title", clamp(title, 50));
        card.put("description", clamp(description, 340));
        if (!buttons.isEmpty()) {
            card.put("buttons", buttons);
            card.put("buttonLayout", "horizontal");
        }
        return Map.of("textCard", card);
    }

    private static List<Map<String, String>> confirmButtons(String confirmLabel) {
        return List.of(messageButton(confirmLabel, "확인"), messageButton("취소", "취소"));
    }

    private static Map<String, String> messageButton(String label, String messageText) {
        return Map.of("action", "message", "label", label, "messageText", messageText);
    }

    private static List<Map<String, String>> quickReplies(String message) {
        List<Map<String, String>> replies = new ArrayList<>();
        if (isSelectionPrompt(message)) {
            Matcher matcher = SELECTION_LINE.matcher(message);
            while (matcher.find() && replies.size() < 5) {
                String number = matcher.group(1);
                String label = selectionLabel(matcher.group(2));
                replies.add(messageButton(label, label));
            }
            replies.add(messageButton("취소", "취소"));
        } else if (isRecoverableError(message)) {
            replies.add(messageButton("다시 시도", "다시 시도"));
            replies.add(messageButton("오늘 일정", "오늘 일정 알려줘"));
        } else if (message.contains("등록된 일정이 없") || message.contains("일정이 없어요")
                || message.contains("일정이 없습니다")
                || message.contains("일정이에요") || isCompleted(message)) {
            replies.add(messageButton("오늘 일정", "오늘 일정 알려줘"));
            replies.add(messageButton("이번 주 일정", "이번 주 일정 알려줘"));
        } else if (message.contains("어느 날짜") || message.contains("날짜를")) {
            replies.add(messageButton("오늘", "오늘"));
            replies.add(messageButton("내일", "내일"));
            replies.add(messageButton("이번 주", "이번 주"));
        }
        return replies;
    }

    private static String selectionLabel(String detail) {
        String compact = detail.replaceAll("\\s+", " ").trim();
        int separator = compact.indexOf(' ');
        return separator > 0 ? compact.substring(0, separator) : compact;
    }

    private static boolean isSelectionPrompt(String message) {
        return message.contains("어떤 일정을 ") && message.contains("할까요?")
                && SELECTION_LINE.matcher(message).find();
    }

    private static boolean isRecoverableError(String message) {
        return message.contains("처리하지 못했") || message.contains("중단되었")
                || message.contains("잠시 후 다시") || message.contains("응답이 늦");
    }

    private static boolean isCompleted(String message) {
        return message.contains("등록했어요") || message.contains("등록 완료")
                || message.contains("등록되었습니다") || message.contains("변경했어요")
                || message.contains("변경 완료") || message.contains("변경했습니다")
                || message.contains("삭제했어요") || message.contains("삭제 완료")
                || message.contains("삭제했습니다");
    }

    private static String completionTitle(String message) {
        if (message.contains("삭제")) return "일정을 삭제했어요";
        if (message.contains("변경")) return "일정을 변경했어요";
        return "일정을 등록했어요";
    }

    private static String beforeInstruction(String message) {
        int instruction = message.lastIndexOf('\n');
        return clamp(instruction > 0 ? message.substring(0, instruction) : message, 340);
    }

    private static String clamp(String value, int maxLength) {
        if (value.length() <= maxLength) return value;
        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }
}
