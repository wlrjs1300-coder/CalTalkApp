package com.caltalk.backend.auth;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Service
public class LogoutService {

    private final String sessionCookieName;
    private final boolean sessionCookieSecure;

    public LogoutService(
            @Value("${server.servlet.session.cookie.name}") String sessionCookieName,
            @Value("${server.servlet.session.cookie.secure:false}") boolean sessionCookieSecure
    ) {
        this.sessionCookieName = sessionCookieName;
        this.sessionCookieSecure = sessionCookieSecure;
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.getContext().setAuthentication(null);
        SecurityContextHolder.clearContext();

        ResponseCookie deletionCookie = ResponseCookie.from(sessionCookieName, "")
                .path("/")
                .maxAge(Duration.ZERO)
                .httpOnly(true)
                .sameSite("Lax")
                .secure(sessionCookieSecure)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deletionCookie.toString());
    }
}
