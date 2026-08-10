package com.caltalk.backend.notification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
class WebPushSender {
    private final ObjectMapper objectMapper;
    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private final String nodeCommand;
    private final Path runnerPath;

    WebPushSender(ObjectMapper objectMapper,
            @Value("${caltalk.push.vapid.public-key:}") String publicKey,
            @Value("${caltalk.push.vapid.private-key:}") String privateKey,
            @Value("${caltalk.push.vapid.subject:mailto:caltalk-local@example.com}") String subject,
            @Value("${caltalk.push.node-command:node}") String nodeCommand,
            @Value("${caltalk.push.runner-path:web-push-runner/send.cjs}") String runnerPath) {
        this.objectMapper = objectMapper;
        this.publicKey = publicKey.trim();
        this.privateKey = privateKey.trim();
        this.subject = subject.trim();
        this.nodeCommand = nodeCommand;
        this.runnerPath = Path.of(runnerPath).toAbsolutePath().normalize();
    }

    boolean available() {
        return !publicKey.isBlank() && !privateKey.isBlank();
    }

    void send(PushSubscription subscription, String payload) throws Exception {
        if (!available()) return;

        ProcessBuilder builder = new ProcessBuilder(nodeCommand, runnerPath.toString()).redirectErrorStream(true);
        Map<String, String> environment = builder.environment();
        environment.put("WEB_PUSH_VAPID_PUBLIC_KEY", publicKey);
        environment.put("WEB_PUSH_VAPID_PRIVATE_KEY", privateKey);
        environment.put("WEB_PUSH_VAPID_SUBJECT", subject);

        Process process = builder.start();
        String request = objectMapper.writeValueAsString(Map.of(
                "endpoint", subscription.getEndpoint(),
                "p256dh", subscription.getP256dh(),
                "auth", subscription.getAuthSecret(),
                "payload", payload));
        try (var input = process.getOutputStream()) {
            input.write(request.getBytes(StandardCharsets.UTF_8));
        }

        boolean completed = process.waitFor(Duration.ofSeconds(20).toMillis(), TimeUnit.MILLISECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new IOException("Web Push sender timed out");
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (process.exitValue() != 0) {
            if (output.startsWith("Web Push 404:") || output.startsWith("Web Push 410:")) {
                throw new PushSubscriptionExpiredException("만료된 기기 알림 구독을 정리했습니다.");
            }
            throw new IOException(output.isBlank() ? "Web Push sender failed" : output);
        }
    }
}
