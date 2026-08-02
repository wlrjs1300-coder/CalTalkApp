package com.caltalk.backend.user;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.auth.LogoutService;
import com.caltalk.backend.common.error.InvalidCredentialsException;
import com.caltalk.backend.common.error.UnauthorizedCurrentUserException;
import com.caltalk.backend.confirmation.ConfirmationRequestRepository;
import com.caltalk.backend.schedule.ScheduleRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final ConfirmationRequestRepository confirmationRequestRepository;
    private final ScheduleRepository scheduleRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogoutService logoutService;

    public AccountDeletionService(
            UserRepository userRepository,
            ConfirmationRequestRepository confirmationRequestRepository,
            ScheduleRepository scheduleRepository,
            PasswordEncoder passwordEncoder,
            LogoutService logoutService
    ) {
        this.userRepository = userRepository;
        this.confirmationRequestRepository = confirmationRequestRepository;
        this.scheduleRepository = scheduleRepository;
        this.passwordEncoder = passwordEncoder;
        this.logoutService = logoutService;
    }

    @Transactional
    public void deleteAccount(
            Authentication authentication,
            DeleteAccountRequest deleteRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        User user = findCurrentUser(authentication, request, response);
        if (!passwordEncoder.matches(deleteRequest.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        confirmationRequestRepository.clearSupersededReferencesByUser(user);
        confirmationRequestRepository.deleteAllByUser(user);
        scheduleRepository.deleteAllByOwner(user);
        userRepository.delete(user);
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

        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logoutService.logout(request, response);
                    return new UnauthorizedCurrentUserException();
                });
    }
}
