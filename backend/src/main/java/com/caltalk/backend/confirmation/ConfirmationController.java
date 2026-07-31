package com.caltalk.backend.confirmation;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.caltalk.backend.schedule.ScheduleResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/confirmations")
public class ConfirmationController {

    private final ConfirmationService confirmationService;

    public ConfirmationController(ConfirmationService confirmationService) {
        this.confirmationService = confirmationService;
    }

    @PostMapping("/{confirmationId}/approve")
    public ResponseEntity<ScheduleResponse> approve(
            @PathVariable Long confirmationId,
            @Valid @RequestBody ApproveConfirmationRequest requestBody,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ScheduleResponse schedule = confirmationService.approve(
                confirmationId,
                requestBody,
                authentication,
                request,
                response
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(schedule);
    }
}
