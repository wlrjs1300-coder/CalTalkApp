package com.caltalk.backend.auth;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SocialAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final SocialLoginService socialLoginService;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SocialAuthenticationFailureHandler failureHandler;
    private final String frontendOrigin;

    public SocialAuthenticationSuccessHandler(
            SocialLoginService socialLoginService,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SocialAuthenticationFailureHandler failureHandler,
            @Value("${caltalk.frontend-origin:http://localhost:5173}") String frontendOrigin
    ) {
        this.socialLoginService = socialLoginService;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.failureHandler = failureHandler;
        this.frontendOrigin = validateOrigin(frontendOrigin);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth)) {
            throw new ServletException("Unexpected social authentication type");
        }
        SocialProfile profile;
        try {
            profile = SocialProfileExtractor.extract(
                    oauth.getAuthorizedClientRegistrationId(),
                    oauth.getPrincipal().getAttributes()
            );
        } catch (AuthenticationException exception) {
            failureHandler.onAuthenticationFailure(request, response, exception);
            return;
        }
        String email = socialLoginService.login(profile);
        Authentication sessionAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                email,
                null,
                List.of()
        );
        sessionAuthenticationStrategy.onAuthentication(sessionAuthentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(sessionAuthentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        response.sendRedirect(frontendOrigin + "/?social=success");
    }

    private static String validateOrigin(String configured) {
        String value = configured.trim().replaceAll("/+$", "");
        URI uri = URI.create(value);
        boolean valid = ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                && uri.getHost() != null
                && uri.getUserInfo() == null
                && (uri.getPath() == null || uri.getPath().isEmpty())
                && uri.getQuery() == null
                && uri.getFragment() == null;
        if (!valid) throw new IllegalArgumentException("Frontend origin must be an exact HTTP(S) origin");
        return value;
    }
}
