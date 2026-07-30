package com.caltalk.backend.user;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.auth.LogoutService;
import com.caltalk.backend.common.error.UnauthorizedCurrentUserException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final LogoutService logoutService;

    public CurrentUserService(UserRepository userRepository, LogoutService logoutService) {
        this.userRepository = userRepository;
        this.logoutService = logoutService;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof String email)
                || email.isBlank()) {
            throw new UnauthorizedCurrentUserException();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logoutService.logout(request, response);
                    return new UnauthorizedCurrentUserException();
                });

        return new CurrentUserResponse(
                user.getEmail(),
                user.getTimezone(),
                user.getCreatedAt()
        );
    }
}
