package com.caltalk.backend.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class OllamaNaturalLanguageProvider implements NaturalLanguageProvider {
    private static final DateTimeFormatter NOW_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE HH:mm VV");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ScheduleCommandValidator validator;
    private final boolean enabled;
    private final URI chatUri;
    private final String model;
    private final Duration timeout;

    @Autowired
    public OllamaNaturalLanguageProvider(
            ObjectMapper objectMapper,
            ScheduleCommandValidator validator,
            @Value("${caltalk.ai.ollama.enabled:true}") boolean enabled,
            @Value("${caltalk.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${caltalk.ai.ollama.model:qwen3:8b}") String model,
            @Value("${caltalk.ai.ollama.timeout:8s}") Duration timeout) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(), objectMapper, validator,
                enabled, URI.create(baseUrl.replaceAll("/+$", "") + "/api/chat"), model, timeout);
    }

    OllamaNaturalLanguageProvider(HttpClient httpClient, ObjectMapper objectMapper,
            ScheduleCommandValidator validator, boolean enabled, URI chatUri, String model, Duration timeout) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.enabled = enabled;
        this.chatUri = chatUri;
        this.model = model;
        this.timeout = timeout;
    }

    public boolean available() {
        return enabled;
    }

    @Override
    public ScheduleCommand analyze(String utterance, AnalysisContext context) {
        if (!available()) throw new NaturalLanguageProviderException("Ollama is disabled.");
        if (utterance == null || utterance.isBlank()) {
            return validator.validate(null);
        }
        try {
            String body = objectMapper.writeValueAsString(requestBody(utterance, context));
            HttpRequest request = HttpRequest.newBuilder(chatUri)
                    .timeout(timeout)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new NaturalLanguageProviderException("Ollama가 HTTP " + response.statusCode() + "을 반환했습니다.");
            }
            JsonNode envelope = objectMapper.readTree(response.body());
            String content = envelope.path("message").path("content").asText();
            if (content.isBlank()) throw new NaturalLanguageProviderException("Ollama 응답에 명령이 없습니다.");
            return validator.validate(objectMapper.readValue(content, ScheduleCommand.class));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new NaturalLanguageProviderException("Ollama 요청이 중단되었습니다.", exception);
        } catch (java.io.IOException | JacksonException exception) {
            throw new NaturalLanguageProviderException("Ollama 일정 분석에 실패했습니다.", exception);
        }
    }

    private Map<String, Object> requestBody(String utterance, AnalysisContext context) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("stream", false);
        request.put("think", false);
        request.put("keep_alive", -1);
        request.put("format", schema());
        request.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt(context)),
                Map.of("role", "user", "content", utterance)));
        request.put("options", Map.of("temperature", 0, "num_predict", 256));
        return request;
    }

    static String systemPrompt(AnalysisContext context) {
        return """
                역할: CalTalk 한국어 일정 명령 분석기. 설명 없이 JSON만 출력한다.
                현재: %s.
                규칙:
                - 추가/등록/잡아줘=CREATE_EVENT, 뭐 있어/알려줘=SEARCH_EVENTS, 옮겨/바꿔=UPDATE_EVENT,
                  삭제/취소=DELETE_EVENT, 언제 비어=FREE_TIME_QUERY.
                - 조회에는 시간이 필수가 아니다.
                - 생성은 제목, 날짜, 시작 시간, 종료 시간이 모두 있어야 READY이다.
                - 수정/삭제 대상이나 변경할 새 시간이 불명확하면 NEEDS_CLARIFICATION이다.
                - 시간 수정은 대상 이름(targetExpression), 날짜(dateExpression), 새 시작·종료 시간이 모두 있어야 READY이다.
                - 삭제할 일정의 날짜와 대상 이름이 모두 있어야 READY이다. 삭제 대상 이름은 targetExpression에 쓴다.
                - dateExpression과 targetExpression은 사용자 표현을 보존한다. 현재 시각을 대신 넣지 않는다.
                - startTime과 endTime은 반드시 24시간제 HH:mm 형식으로 반환한다.
                - 질문과 제목은 반드시 한국어이다.
                - 입력에 '이전 일정 요청'이 있으면 그 값과 '사용자의 새 답변'을 합쳐 완성된 명령을 만든다.
                예: '내일 3시에 팀 미팅 추가해줘' => CREATE_EVENT, NEEDS_CLARIFICATION, title='팀 미팅',
                    dateExpression='내일', startTime='15:00', endTime=null.
                예: '내일 3시부터 4시까지 팀 미팅 추가해줘' => CREATE_EVENT, READY, title='팀 미팅',
                    dateExpression='내일', startTime='15:00', endTime='16:00'.
                예: '금요일에 뭐 있어?' => SEARCH_EVENTS, READY, dateExpression='이번 주 금요일'.
                예: '미팅 시간 바꿔줘' => UPDATE_EVENT, NEEDS_CLARIFICATION,
                    missingFields=['targetEvent','newDateTime'], userFacingQuestion='어떤 미팅을 언제로 변경할까요?'.
                """.formatted(context.now().format(NOW_FORMAT));
    }

    static Map<String, Object> schema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("schemaVersion", Map.of("type", "string"));
        properties.put("intent", Map.of("type", "string", "enum", List.of(
                "SEARCH_EVENTS", "CREATE_EVENT", "UPDATE_EVENT", "DELETE_EVENT", "FREE_TIME_QUERY", "UNKNOWN")));
        properties.put("status", Map.of("type", "string", "enum", List.of(
                "READY", "NEEDS_CLARIFICATION", "UNSUPPORTED")));
        for (String field : List.of("title", "dateExpression", "startTime", "endTime", "targetExpression", "userFacingQuestion")) {
            properties.put(field, Map.of("type", List.of("string", "null")));
        }
        properties.put("missingFields", Map.of("type", "array", "items", Map.of("type", "string")));
        properties.put("ambiguities", Map.of("type", "array", "items", Map.of("type", "string")));
        return Map.of(
                "type", "object",
                "properties", properties,
                "required", List.copyOf(properties.keySet()),
                "additionalProperties", false);
    }
}
