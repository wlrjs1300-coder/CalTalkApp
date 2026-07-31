package com.caltalk.backend.user;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    public UserController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
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
}
