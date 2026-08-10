package com.caltalk.backend.user;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final CurrentUserService currentUserService;
    private final AccountDeletionService accountDeletionService;

    public UserController(
            CurrentUserService currentUserService,
            AccountDeletionService accountDeletionService
    ) {
        this.currentUserService = currentUserService;
        this.accountDeletionService = accountDeletionService;
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> currentUser(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        CurrentUserResponse currentUser = currentUserService.getCurrentUser(
                authentication,
                request,
                response
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(currentUser);
    }

    @PatchMapping("/me")
    public ResponseEntity<CurrentUserResponse> updateTimezone(
            Authentication authentication,
            @Valid @RequestBody UpdateTimezoneRequest updateRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        CurrentUserResponse currentUser = currentUserService.updateTimezone(
                authentication,
                updateRequest,
                request,
                response
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(currentUser);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            Authentication authentication,
            @Valid @RequestBody DeleteAccountRequest deleteRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        accountDeletionService.deleteAccount(
                authentication,
                deleteRequest,
                request,
                response
        );
        currentUserService.logout(request, response);
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .build();
    }

    @PatchMapping("/me/chat-preferences")
    public ResponseEntity<CurrentUserResponse> updateChatPreferences(
            Authentication authentication,
            @Valid @RequestBody UpdateChatPreferencesRequest updateRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(currentUserService.updateChatPreferences(
                        authentication, updateRequest, request, response));
    }
}
