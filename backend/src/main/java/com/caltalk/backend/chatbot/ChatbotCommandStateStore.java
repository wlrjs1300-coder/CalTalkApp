package com.caltalk.backend.chatbot;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.caltalk.backend.ai.ScheduleCommand;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class ChatbotCommandStateStore {
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String PREFIX = "caltalk:kakao:pending:";
    private static final String DELETE_PREFIX = "caltalk:kakao:pending-delete:";
    private static final String UPDATE_PREFIX = "caltalk:kakao:pending-update:";
    private static final String SELECTION_PREFIX = "caltalk:kakao:pending-selection:";
    private static final String LAST_REQUEST_PREFIX = "caltalk:kakao:last-request:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ChatbotCommandStateStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void save(String protectedUserKey, ScheduleCommand command) {
        try {
            redis.opsForValue().set(PREFIX + protectedUserKey, objectMapper.writeValueAsString(command), TTL);
        } catch (JacksonException exception) {
            throw new IllegalStateException("카카오 명령 상태를 저장할 수 없습니다.", exception);
        }
    }

    public Optional<ScheduleCommand> get(String protectedUserKey) {
        String value = redis.opsForValue().get(PREFIX + protectedUserKey);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, ScheduleCommand.class));
        } catch (JacksonException exception) {
            delete(protectedUserKey);
            return Optional.empty();
        }
    }

    public Optional<ScheduleCommand> take(String protectedUserKey) {
        String value = redis.opsForValue().getAndDelete(PREFIX + protectedUserKey);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, ScheduleCommand.class));
        } catch (JacksonException exception) {
            return Optional.empty();
        }
    }

    public void delete(String protectedUserKey) {
        redis.delete(PREFIX + protectedUserKey);
    }

    public void saveDelete(String protectedUserKey, PendingDelete pending) {
        try {
            redis.opsForValue().set(DELETE_PREFIX + protectedUserKey,
                    objectMapper.writeValueAsString(pending), TTL);
        } catch (JacksonException exception) {
            throw new IllegalStateException("카카오 삭제 상태를 저장할 수 없습니다.", exception);
        }
    }

    public Optional<PendingDelete> getDelete(String protectedUserKey) {
        String value = redis.opsForValue().get(DELETE_PREFIX + protectedUserKey);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, PendingDelete.class));
        } catch (JacksonException exception) {
            deleteDelete(protectedUserKey);
            return Optional.empty();
        }
    }

    public Optional<PendingDelete> takeDelete(String protectedUserKey) {
        String value = redis.opsForValue().getAndDelete(DELETE_PREFIX + protectedUserKey);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, PendingDelete.class));
        } catch (JacksonException exception) {
            return Optional.empty();
        }
    }

    public void deleteDelete(String protectedUserKey) {
        redis.delete(DELETE_PREFIX + protectedUserKey);
    }

    public void saveUpdate(String protectedUserKey, PendingUpdate pending) {
        try {
            redis.opsForValue().set(UPDATE_PREFIX + protectedUserKey,
                    objectMapper.writeValueAsString(pending), TTL);
        } catch (JacksonException exception) {
            throw new IllegalStateException("카카오 수정 상태를 저장할 수 없습니다.", exception);
        }
    }

    public Optional<PendingUpdate> getUpdate(String protectedUserKey) {
        String value = redis.opsForValue().get(UPDATE_PREFIX + protectedUserKey);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, PendingUpdate.class));
        } catch (JacksonException exception) {
            deleteUpdate(protectedUserKey);
            return Optional.empty();
        }
    }

    public Optional<PendingUpdate> takeUpdate(String protectedUserKey) {
        String value = redis.opsForValue().getAndDelete(UPDATE_PREFIX + protectedUserKey);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, PendingUpdate.class));
        } catch (JacksonException exception) {
            return Optional.empty();
        }
    }

    public void deleteUpdate(String protectedUserKey) {
        redis.delete(UPDATE_PREFIX + protectedUserKey);
    }

    public void saveSelection(String protectedUserKey, PendingSelection pending) {
        try {
            redis.opsForValue().set(SELECTION_PREFIX + protectedUserKey,
                    objectMapper.writeValueAsString(pending), TTL);
        } catch (JacksonException exception) {
            throw new IllegalStateException("일정 후보 선택 상태를 저장할 수 없습니다.", exception);
        }
    }

    public Optional<PendingSelection> getSelection(String protectedUserKey) {
        String value = redis.opsForValue().get(SELECTION_PREFIX + protectedUserKey);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, PendingSelection.class));
        } catch (JacksonException exception) {
            deleteSelection(protectedUserKey);
            return Optional.empty();
        }
    }

    public void deleteSelection(String protectedUserKey) {
        redis.delete(SELECTION_PREFIX + protectedUserKey);
    }

    public void saveLastRequest(String protectedUserKey, String utterance) {
        if (utterance == null || utterance.isBlank()) return;
        redis.opsForValue().set(LAST_REQUEST_PREFIX + protectedUserKey, utterance.trim(), TTL);
    }

    public Optional<String> getLastRequest(String protectedUserKey) {
        return Optional.ofNullable(redis.opsForValue().get(LAST_REQUEST_PREFIX + protectedUserKey));
    }

    public record PendingDelete(Long scheduleId, Long version, String title, String dateLabel) {
    }

    public record PendingUpdate(Long scheduleId, Long version, String title,
            String newStartAt, String newEndAt, String dateLabel) {
    }

    public record PendingSelection(SelectionAction action, List<SelectionCandidate> candidates,
            String newStartAt, String newEndAt, String dateLabel) {
        public PendingSelection {
            candidates = List.copyOf(candidates);
        }
    }

    public record SelectionCandidate(Long scheduleId, Long version, String title,
            String startAt, String endAt) {
    }

    public enum SelectionAction {
        DELETE,
        UPDATE
    }
}
