package com.caltalk.backend.chatbot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import tools.jackson.databind.ObjectMapper;

@Service
public class KakaoCallbackService {
    private static final Logger log = LoggerFactory.getLogger(KakaoCallbackService.class);
    private static final int MAX_TEXT_LENGTH = 1_000;
    private static final int MAX_DELIVERY_ATTEMPTS = 3;
    private static final long[] RETRY_DELAYS_MILLIS = {250L, 750L};

    private final KakaoScheduleAssistantService assistantService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ExecutorService executor;

    @Autowired
    public KakaoCallbackService(KakaoScheduleAssistantService assistantService, ObjectMapper objectMapper) {
        this(assistantService, objectMapper,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                Executors.newVirtualThreadPerTaskExecutor());
    }

    KakaoCallbackService(KakaoScheduleAssistantService assistantService, ObjectMapper objectMapper,
            HttpClient httpClient, ExecutorService executor) {
        this.assistantService = assistantService;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.executor = executor;
    }

    public boolean supports(String callbackUrl) {
        return allowedCallbackUri(callbackUrl) != null;
    }

    public void dispatch(String callbackUrl, String externalUserId, String utterance) {
        URI callbackUri = allowedCallbackUri(callbackUrl);
        if (callbackUri == null) throw new IllegalArgumentException("허용되지 않은 카카오 callback URL입니다.");
        executor.execute(() -> deliver(callbackUri, externalUserId, utterance));
    }

    public Optional<String> replyWithinOrDispatch(String callbackUrl, String externalUserId,
            String utterance, Duration directResponseWindow) {
        URI callbackUri = allowedCallbackUri(callbackUrl);
        if (callbackUri == null) throw new IllegalArgumentException("허용되지 않은 카카오 callback URL입니다.");
        Future<String> reply = executor.submit(() -> assistantService.reply(externalUserId, utterance));
        try {
            return Optional.of(reply.get(directResponseWindow.toMillis(), TimeUnit.MILLISECONDS));
        } catch (TimeoutException exception) {
            executor.execute(() -> deliverCompletedReply(callbackUri, reply));
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.of("요청 처리가 중단됐어요. 잠시 후 다시 시도해 주세요.");
        } catch (ExecutionException exception) {
            log.warn("Kakao direct reply failed: {}", exception.getCause().getClass().getSimpleName());
            return Optional.of("요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.");
        }
    }

    private void deliverCompletedReply(URI callbackUri, Future<String> reply) {
        try {
            deliverMessage(callbackUri, reply.get());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Kakao callback delivery was interrupted");
        } catch (ExecutionException exception) {
            log.warn("Kakao callback reply failed: {}", exception.getCause().getClass().getSimpleName());
        } catch (Exception exception) {
            log.warn("Kakao callback delivery failed: {}", exception.getClass().getSimpleName());
        }
    }

    private void deliver(URI callbackUri, String externalUserId, String utterance) {
        try {
            deliverMessage(callbackUri, assistantService.reply(externalUserId, utterance));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Kakao callback delivery was interrupted");
        } catch (Exception exception) {
            log.warn("Kakao callback delivery failed: {}", exception.getClass().getSimpleName());
        }
    }

    private void deliverMessage(URI callbackUri, String replyMessage) throws Exception {
        String message = replyMessage;
        if (message.length() > MAX_TEXT_LENGTH) message = message.substring(0, MAX_TEXT_LENGTH);
        Map<String, Object> payload = KakaoSkillResponseFactory.response(message);
        HttpRequest request = HttpRequest.newBuilder(callbackUri)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        long startedAt = System.nanoTime();
        for (int attempt = 1; attempt <= MAX_DELIVERY_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
                int status = response.statusCode();
                if (status / 100 == 2) {
                    log.info("Kakao callback delivered attempt={} elapsedMs={}", attempt, elapsedMillis(startedAt));
                    return;
                }
                if (!retryableStatus(status) || attempt == MAX_DELIVERY_ATTEMPTS) {
                    log.warn("Kakao callback rejected status={} attempt={} elapsedMs={}",
                            status, attempt, elapsedMillis(startedAt));
                    return;
                }
                log.warn("Kakao callback retry scheduled status={} attempt={}", status, attempt);
            } catch (java.io.IOException exception) {
                if (attempt == MAX_DELIVERY_ATTEMPTS) throw exception;
                log.warn("Kakao callback retry scheduled cause={} attempt={}",
                        exception.getClass().getSimpleName(), attempt);
            }
            Thread.sleep(RETRY_DELAYS_MILLIS[attempt - 1]);
        }
    }

    private static boolean retryableStatus(int status) {
        return status == 408 || status == 425 || status == 429 || status >= 500;
    }

    private static long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    static URI allowedCallbackUri(String callbackUrl) {
        if (callbackUrl == null || callbackUrl.isBlank()) return null;
        try {
            URI uri = URI.create(callbackUrl);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null
                    || uri.getFragment() != null) return null;
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            if (!normalizedHost.equals("kakao.com") && !normalizedHost.endsWith(".kakao.com")) return null;
            return uri;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @PreDestroy
    void shutdown() {
        executor.close();
    }
}
