package com.caltalk.backend.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/push")
class PushSubscriptionController {
    private final PushSubscriptionService service;
    PushSubscriptionController(PushSubscriptionService service) { this.service = service; }

    @GetMapping("/config")
    PushSubscriptionService.PushConfigResponse config() { return service.config(); }

    @PostMapping("/subscriptions")
    ResponseEntity<Void> subscribe(Authentication auth, @Valid @RequestBody PushSubscriptionRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        service.subscribe(auth, body, request, response);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/subscriptions")
    ResponseEntity<Void> unsubscribe(Authentication auth, @Valid @RequestBody PushSubscriptionRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        service.unsubscribe(auth, body, request, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    PushSubscriptionService.PushTestResponse test(Authentication auth, HttpServletRequest request, HttpServletResponse response) {
        return service.sendTest(auth, request, response);
    }
}
