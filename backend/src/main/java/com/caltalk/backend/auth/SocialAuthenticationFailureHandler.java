package com.caltalk.backend.auth;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SocialAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final String frontendOrigin;

    public SocialAuthenticationFailureHandler(
            @Value("${caltalk.frontend-origin:http://localhost:5173}") String frontendOrigin
    ) {
        this.frontendOrigin = frontendOrigin.trim().replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String errorCode = exception instanceof OAuth2AuthenticationException oauthException
                ? oauthException.getError().getErrorCode()
                : "authentication_failed";
        String reason = switch (errorCode) {
            case "email_not_verified", "profile_missing" -> "email";
            default -> "failed";
        };
        response.sendRedirect(frontendOrigin + "/login?social=" + reason);
    }
}
