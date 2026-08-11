package com.caltalk.backend.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class KakaoSkillResponseFactoryTests {
    @SuppressWarnings("unchecked")
    @Test
    void createsConfirmationCardWithMessageButtons() {
        Map<String, Object> response = KakaoSkillResponseFactory.response(
                "팀 회의\n내일 10:00부터 11:00까지\n등록하려면 '확인', 취소하려면 '취소'라고 답해 주세요.");
        Map<String, Object> template = (Map<String, Object>) response.get("template");
        List<Map<String, Object>> outputs = (List<Map<String, Object>>) template.get("outputs");
        Map<String, Object> card = (Map<String, Object>) outputs.getFirst().get("textCard");
        List<Map<String, String>> buttons = (List<Map<String, String>>) card.get("buttons");

        assertThat(card.get("title")).isEqualTo("일정을 등록할까요?");
        assertThat(buttons).extracting(button -> button.get("messageText"))
                .containsExactly("확인", "취소");
    }

    @SuppressWarnings("unchecked")
    @Test
    void keepsOrdinaryErrorsAsSimpleText() {
        Map<String, Object> response = KakaoSkillResponseFactory.response("요청을 처리하지 못했어요.");
        Map<String, Object> template = (Map<String, Object>) response.get("template");
        List<Map<String, Object>> outputs = (List<Map<String, Object>>) template.get("outputs");

        assertThat(outputs.getFirst()).containsKey("simpleText");
    }

    @SuppressWarnings("unchecked")
    @Test
    void addsCandidateSelectionQuickReplies() {
        Map<String, Object> response = KakaoSkillResponseFactory.response("""
                🗑️ 어떤 일정을 변경할까요?
                1. 10:00~11:00  팀 회의
                2. 15:00~16:00  고객 미팅
                번호나 시간을 말씀해 주세요. 취소하려면 '취소'라고 답해 주세요.
                """);
        Map<String, Object> template = (Map<String, Object>) response.get("template");
        List<Map<String, String>> replies = (List<Map<String, String>>) template.get("quickReplies");

        assertThat(replies).extracting(reply -> reply.get("messageText"))
                .containsExactly("10:00~11:00", "15:00~16:00", "취소");
        assertThat(replies).extracting(reply -> reply.get("label"))
                .containsExactly("10:00~11:00", "15:00~16:00", "취소");
    }

    @SuppressWarnings("unchecked")
    @Test
    void addsRecoveryActionsForTemporaryFailure() {
        Map<String, Object> response = KakaoSkillResponseFactory.response(
                "일정 문장을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.");
        Map<String, Object> template = (Map<String, Object>) response.get("template");
        List<Map<String, String>> replies = (List<Map<String, String>>) template.get("quickReplies");

        assertThat(replies).extracting(reply -> reply.get("messageText"))
                .containsExactly("다시 시도", "오늘 일정 알려줘");
    }
}
