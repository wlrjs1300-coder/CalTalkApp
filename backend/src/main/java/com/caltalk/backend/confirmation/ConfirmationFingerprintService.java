package com.caltalk.backend.confirmation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.caltalk.backend.schedule.Schedule;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ConfirmationFingerprintService {

    private final ObjectMapper objectMapper;

    public ConfirmationFingerprintService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String candidate(
            String title,
            Instant startAt,
            Instant endAt,
            String location
    ) {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("command_type", "CREATE_EVENT");
        candidate.put("target_schedule_id", null);
        candidate.put("title", title);
        candidate.put("start_at", startAt.toString());
        candidate.put("end_at", endAt.toString());
        candidate.put("location_action", location == null ? "REMOVE" : "SET");
        candidate.put("location_value", location);
        return sha256(json(candidate));
    }

    public String conflicts(List<Schedule> schedules) {
        String normalized = schedules.stream()
                .map(schedule -> schedule.getId() + ":" + schedule.getVersion())
                .reduce("", (left, right) -> left + right + ";");
        return sha256(normalized);
    }

    public String updateCandidate(
            Long targetScheduleId,
            String title,
            Instant startAt,
            Instant endAt,
            String locationAction,
            String locationValue
    ) {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("command_type", "UPDATE_EVENT");
        candidate.put("target_schedule_id", targetScheduleId);
        candidate.put("title", title);
        candidate.put("start_at", startAt == null ? null : startAt.toString());
        candidate.put("end_at", endAt == null ? null : endAt.toString());
        candidate.put("location_action", locationAction);
        candidate.put("location_value", locationValue);
        return sha256(json(candidate));
    }

    private String json(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("후보 정규화에 실패했습니다.", exception);
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
