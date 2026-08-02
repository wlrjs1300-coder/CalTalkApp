package com.caltalk.backend.user;

import java.time.ZoneId;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.auth.LogoutService;
import com.caltalk.backend.common.error.InvalidTimezoneException;
import com.caltalk.backend.common.error.UnauthorizedCurrentUserException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class CurrentUserService {

    private static final Set<String> REGION_TIMEZONE_IDS = ZoneId.getAvailableZoneIds()
            .stream()
            .filter(timezone -> timezone.contains("/"))
            .collect(Collectors.toUnmodifiableSet());

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
        User user = findCurrentUser(authentication, request, response);
        return toResponse(user);
    }

    @Transactional
    public CurrentUserResponse updateTimezone(
            Authentication authentication,
            UpdateTimezoneRequest updateRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        User user = findCurrentUser(authentication, request, response);
        String timezone = updateRequest.timezone();
        if (!REGION_TIMEZONE_IDS.contains(timezone)) {
            throw new InvalidTimezoneException();
        }

        user.changeTimezone(timezone);
        return toResponse(user);
    }

    private User findCurrentUser(
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
        return user;
    }

    private CurrentUserResponse toResponse(User user) {
        return new CurrentUserResponse(
                user.getEmail(),
                user.getTimezone(),
                user.getCreatedAt()
        );
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        logoutService.logout(request, response);
    }
}
