package com.caltalk.backend.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import tools.jackson.databind.ObjectMapper;

class ChatbotCommandStateStoreTests {

    @SuppressWarnings("unchecked")
    @Test
    void consumesPendingDeleteWithSingleAtomicRedisOperation() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.getAndDelete("caltalk:kakao:pending-delete:protected-user"))
                .thenReturn("""
                        {"scheduleId":59,"version":2,"title":"팀 회의","dateLabel":"8월 10일"}
                        """);
        ChatbotCommandStateStore store = new ChatbotCommandStateStore(redis, new ObjectMapper());

        var pending = store.takeDelete("protected-user");

        assertThat(pending).isPresent();
        assertThat(pending.orElseThrow().scheduleId()).isEqualTo(59L);
        verify(values).getAndDelete("caltalk:kakao:pending-delete:protected-user");
    }
}
