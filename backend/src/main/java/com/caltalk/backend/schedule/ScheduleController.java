package com.caltalk.backend.schedule;

import java.net.URI;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateScheduleRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ScheduleResponse schedule = scheduleService.create(
                authentication,
                requestBody,
                request,
                response
        );
        return ResponseEntity.created(URI.create("/api/v1/schedules/" + schedule.id()))
                .cacheControl(CacheControl.noStore())
                .body(schedule);
    }
}
