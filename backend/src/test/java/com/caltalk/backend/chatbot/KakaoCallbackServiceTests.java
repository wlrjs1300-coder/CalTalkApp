package com.caltalk.backend.chatbot;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KakaoCallbackServiceTests {

    @Test
    void acceptsOnlySecureKakaoCallbackUrls() {
        assertThat(KakaoCallbackService.allowedCallbackUri(
                "https://bot-api.kakao.com/callback/one-time-token")).isNotNull();
        assertThat(KakaoCallbackService.allowedCallbackUri("http://bot-api.kakao.com/callback/token")).isNull();
        assertThat(KakaoCallbackService.allowedCallbackUri("https://kakao.com.attacker.example/callback")).isNull();
        assertThat(KakaoCallbackService.allowedCallbackUri("https://127.0.0.1/internal")).isNull();
        assertThat(KakaoCallbackService.allowedCallbackUri("not-a-url")).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    void analyzesAndPostsFinalMessageToOneTimeCallbackUrl() throws Exception {
        KakaoScheduleAssistantService assistant = mock(KakaoScheduleAssistantService.class);
        HttpClient client = mock(HttpClient.class);
        ExecutorService executor = mock(ExecutorService.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(assistant.reply("bot-user-1", "내일 일정 알려줘")).thenReturn("내일 일정이 없어요.");
        when(response.statusCode()).thenReturn(200);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        KakaoCallbackService service = new KakaoCallbackService(
                assistant, new ObjectMapper(), client, executor);

        service.dispatch("https://bot-api.kakao.com/callback/one-time-token",
                "bot-user-1", "내일 일정 알려줘");

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(request.getValue().uri().toString())
                .isEqualTo("https://bot-api.kakao.com/callback/one-time-token");
        assertThat(request.getValue().method()).isEqualTo("POST");
        verify(assistant).reply("bot-user-1", "내일 일정 알려줘");
    }
}
