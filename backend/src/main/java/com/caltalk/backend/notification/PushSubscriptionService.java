package com.caltalk.backend.notification;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.schedule.CurrentScheduleUserService;
import com.caltalk.backend.user.User;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
class PushSubscriptionService {
    private final PushSubscriptionRepository repository;
    private final CurrentScheduleUserService users;
    private final String publicKey;
    private final WebPushSender sender;
    private final ObjectMapper objectMapper;

    PushSubscriptionService(PushSubscriptionRepository repository, CurrentScheduleUserService users,
            @Value("${caltalk.push.vapid.public-key:}") String publicKey, WebPushSender sender,
            ObjectMapper objectMapper) {
        this.repository = repository; this.users = users; this.publicKey = publicKey.trim();
        this.sender = sender; this.objectMapper = objectMapper;
    }

    PushConfigResponse config() { return new PushConfigResponse(!publicKey.isBlank(), publicKey); }

    @Transactional
    void subscribe(Authentication authentication, PushSubscriptionRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        User user = users.requireCurrentUser(authentication, request, response);
        validateEndpoint(body.endpoint());
        PushSubscription subscription = repository.findByEndpoint(body.endpoint())
                .orElseGet(() -> new PushSubscription(user, body.endpoint(), body.keys().p256dh(), body.keys().auth()));
        subscription.refresh(user, body.keys().p256dh(), body.keys().auth());
        repository.save(subscription);
    }

    @Transactional
    void unsubscribe(Authentication authentication, PushSubscriptionRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        User user = users.requireCurrentUser(authentication, request, response);
        repository.deleteByEndpointAndUser(body.endpoint(), user);
    }

    @Transactional
    PushTestResponse sendTest(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        User user = users.requireCurrentUser(authentication, request, response);
        String payload = objectMapper.writeValueAsString(java.util.Map.of(
                "title", "🔔 CalTalk 알림 연결 완료",
                "body", "이 기기에서 일정 알림을 받을 수 있어요.",
                "url", "/", "tag", "caltalk-test"));
        int sent = 0;
        int failed = 0;
        String lastError = null;
        for (PushSubscription subscription : repository.findAllByUser(user)) {
            try { sender.send(subscription, payload); sent += 1; }
            catch (PushSubscriptionExpiredException exception) {
                repository.delete(subscription);
                failed += 1;
                lastError = exception.getMessage();
            }
            catch (Exception exception) { failed += 1; lastError = exception.getMessage(); }
        }
        return new PushTestResponse(sent, failed, lastError);
    }

    private static void validateEndpoint(String value) {
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("유효하지 않은 푸시 구독 주소입니다.");
        }
    }

    record PushConfigResponse(boolean available, String publicKey) {}
    record PushTestResponse(int sent, int failed, String error) {}
}
