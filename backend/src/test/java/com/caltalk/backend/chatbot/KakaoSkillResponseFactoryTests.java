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
}
