package com.caltalk.backend.schedule;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.caltalk.backend.auth.LogoutService;
import com.caltalk.backend.common.error.UnauthorizedCurrentUserException;
import com.caltalk.backend.user.User;
import com.caltalk.backend.user.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class CurrentScheduleUserService {

    private final UserRepository userRepository;
    private final LogoutService logoutService;

    public CurrentScheduleUserService(
            UserRepository userRepository,
            LogoutService logoutService
    ) {
        this.userRepository = userRepository;
        this.logoutService = logoutService;
    }

    public User requireCurrentUser(
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

        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logoutService.logout(request, response);
                    return new UnauthorizedCurrentUserException();
                });
    }
}
