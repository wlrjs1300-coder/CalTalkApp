package com.caltalk.backend.chatbot;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.caltalk.backend.config.SecurityConfig;
import com.caltalk.backend.kakao.KakaoLinkService;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KakaoChatbotController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "caltalk.chatbot.kakao.enabled=true",
        "caltalk.chatbot.kakao.skill-secret=test-skill-secret"
})
class KakaoChatbotControllerTests {

    private static final String PAYLOAD = """
            {
              "userRequest": {
                "utterance": "내일 일정 알려줘",
                "user": {"id": "bot-user-1"}
              },
              "unknownFutureField": true
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KakaoLinkService linkService;

    @MockitoBean
    private KakaoScheduleAssistantService assistantService;

    @MockitoBean
    private KakaoCallbackService callbackService;

    @Test
    void acceptsKakaoSkillPayloadWithDedicatedSecret() throws Exception {
        when(linkService.consume("bot-user-1", "내일 일정 알려줘"))
                .thenReturn(KakaoLinkService.LinkResult.NOT_A_CODE);
        mockMvc.perform(post("/api/v1/kakao/skill")
                        .header("X-CalTalk-Skill-Secret", "test-skill-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("2.0"))
                .andExpect(jsonPath("$.template.outputs[0].simpleText.text")
                        .value("먼저 CalTalk 계정을 연결해 주세요. CalTalk 설정에서 8자리 연결 코드를 발급한 뒤 이 채팅방에 입력해 주세요."));
    }

    @Test
    void routesLinkedUsersUtteranceToScheduleAssistant() throws Exception {
        when(linkService.consume("bot-user-1", "내일 일정 알려줘"))
                .thenReturn(KakaoLinkService.LinkResult.ALREADY_CONNECTED);
        when(assistantService.reply("bot-user-1", "내일 일정 알려줘"))
                .thenReturn("8월 5일에는 등록된 일정이 없어요.");

        mockMvc.perform(post("/api/v1/kakao/skill")
                        .header("X-CalTalk-Skill-Secret", "test-skill-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.template.outputs[0].simpleText.text")
                        .value("8월 5일에는 등록된 일정이 없어요."));
    }

    @Test
    void acknowledgesCallbackImmediatelyAndDispatchesWork() throws Exception {
        String callbackPayload = """
                {
                  "userRequest": {
                    "utterance": "내일 일정 알려줘",
                    "callbackUrl": "https://bot-api.kakao.com/callback/one-time-token",
                    "user": {"id": "bot-user-1"}
                  }
                }
                """;
        when(linkService.consume("bot-user-1", "내일 일정 알려줘"))
                .thenReturn(KakaoLinkService.LinkResult.ALREADY_CONNECTED);
        when(callbackService.supports("https://bot-api.kakao.com/callback/one-time-token")).thenReturn(true);
        when(callbackService.replyWithinOrDispatch(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/kakao/skill")
                        .header("X-CalTalk-Skill-Secret", "test-skill-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("2.0"))
                .andExpect(jsonPath("$.useCallback").value(true))
                .andExpect(jsonPath("$.template").doesNotExist());

        verify(callbackService).replyWithinOrDispatch(
                "https://bot-api.kakao.com/callback/one-time-token",
                "bot-user-1", "내일 일정 알려줘", Duration.ofMillis(3_500));
        verify(assistantService, never()).reply("bot-user-1", "내일 일정 알려줘");
    }

    @Test
    void returnsDirectlyWhenAnalysisFinishesInsideResponseWindow() throws Exception {
        String callbackPayload = """
                {
                  "userRequest": {
                    "utterance": "내일 일정 알려줘",
                    "callbackUrl": "https://bot-api.kakao.com/callback/one-time-token",
                    "user": {"id": "bot-user-1"}
                  }
                }
                """;
        when(linkService.consume("bot-user-1", "내일 일정 알려줘"))
                .thenReturn(KakaoLinkService.LinkResult.ALREADY_CONNECTED);
        when(callbackService.supports("https://bot-api.kakao.com/callback/one-time-token")).thenReturn(true);
        when(callbackService.replyWithinOrDispatch(any(), any(), any(), any()))
                .thenReturn(Optional.of("내일 일정이 없어요."));

        mockMvc.perform(post("/api/v1/kakao/skill")
                        .header("X-CalTalk-Skill-Secret", "test-skill-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.template.outputs[0].simpleText.text")
                        .value("내일 일정이 없어요."))
                .andExpect(jsonPath("$.useCallback").doesNotExist());
    }

    @Test
    void answersFastFollowUpSynchronouslyEvenWhenCallbackIsAvailable() throws Exception {
        String callbackPayload = """
                {
                  "userRequest": {
                    "utterance": "확인",
                    "callbackUrl": "https://bot-api.kakao.com/callback/one-time-token",
                    "user": {"id": "bot-user-1"}
                  }
                }
                """;
        when(linkService.consume("bot-user-1", "확인"))
                .thenReturn(KakaoLinkService.LinkResult.ALREADY_CONNECTED);
        when(callbackService.supports("https://bot-api.kakao.com/callback/one-time-token")).thenReturn(true);
        when(assistantService.reply("bot-user-1", "확인")).thenReturn("등록했어요.");

        mockMvc.perform(post("/api/v1/kakao/skill")
                        .header("X-CalTalk-Skill-Secret", "test-skill-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.template.outputs[0].textCard.title").value("일정을 등록했어요"))
                .andExpect(jsonPath("$.template.quickReplies[0].label").value("오늘 일정"))
                .andExpect(jsonPath("$.useCallback").doesNotExist());

        verify(callbackService, never()).dispatch("https://bot-api.kakao.com/callback/one-time-token",
                "bot-user-1", "확인");
        verify(assistantService).reply("bot-user-1", "확인");
    }

    @Test
    void rejectsMissingSkillSecret() throws Exception {
        mockMvc.perform(post("/api/v1/kakao/skill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsMalformedPayload() throws Exception {
        mockMvc.perform(post("/api/v1/kakao/skill")
                        .header("X-CalTalk-Skill-Secret", "test-skill-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userRequest\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.version").value("2.0"));
    }
}
