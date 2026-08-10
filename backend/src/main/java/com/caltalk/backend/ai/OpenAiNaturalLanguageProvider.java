package com.caltalk.backend.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class OpenAiNaturalLanguageProvider implements NaturalLanguageProvider {
    private static final URI RESPONSES_URI = URI.create("https://api.openai.com/v1/responses");

    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final ScheduleCommandValidator validator;
    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    @Autowired
    public OpenAiNaturalLanguageProvider(ObjectMapper objectMapper, ScheduleCommandValidator validator,
            @Value("${caltalk.ai.openai.enabled:false}") boolean enabled,
            @Value("${caltalk.ai.openai.api-key:}") String apiKey,
            @Value("${caltalk.ai.openai.model:gpt-5.6-sol}") String model,
            @Value("${caltalk.ai.openai.timeout:8s}") Duration timeout) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(), objectMapper, validator,
                enabled, apiKey, model, timeout);
    }

    OpenAiNaturalLanguageProvider(HttpClient client, ObjectMapper objectMapper, ScheduleCommandValidator validator,
            boolean enabled, String apiKey, String model, Duration timeout) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
        this.timeout = timeout;
    }

    public boolean available() {
        return enabled && !apiKey.isBlank();
    }

    @Override
    public ScheduleCommand analyze(String utterance, AnalysisContext context) {
        if (!available()) throw new NaturalLanguageProviderException("OpenAI fallback is disabled.");
        try {
            String json = objectMapper.writeValueAsString(requestBody(utterance, context));
            HttpRequest request = HttpRequest.newBuilder(RESPONSES_URI)
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json)).build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new NaturalLanguageProviderException("OpenAI returned HTTP " + response.statusCode() + ".");
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = extractOutputText(root);
            if (content == null || content.isBlank()) {
                throw new NaturalLanguageProviderException("OpenAI returned an empty response.");
            }
            return validator.validate(objectMapper.readValue(content, ScheduleCommand.class));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new NaturalLanguageProviderException("OpenAI request was interrupted.", exception);
        } catch (java.io.IOException | JacksonException exception) {
            throw new NaturalLanguageProviderException("OpenAI schedule analysis failed.", exception);
        }
    }

    private Map<String, Object> requestBody(String utterance, AnalysisContext context) {
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "schedule_command");
        format.put("strict", true);
        format.put("schema", OllamaNaturalLanguageProvider.schema());
        return Map.of(
                "model", model,
                "reasoning", Map.of("effort", "low"),
                "instructions", OllamaNaturalLanguageProvider.systemPrompt(context),
                "input", utterance,
                "max_output_tokens", 512,
                "text", Map.of("format", format));
    }

    private static String extractOutputText(JsonNode root) {
        for (JsonNode output : root.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())) return content.path("text").asText();
            }
        }
        return null;
    }
}
