package com.caltalk.backend.schedule;

import java.net.URI;
import java.time.OffsetDateTime;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public ResponseEntity<ScheduleListResponse> findInRange(
            Authentication authentication,
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ScheduleListResponse schedules = scheduleService.findInRange(
                authentication,
                from.toInstant(),
                to.toInstant(),
                request,
                response
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(schedules);
    }

    @GetMapping("/{scheduleId}")
    public ResponseEntity<ScheduleResponse> findById(
            Authentication authentication,
            @PathVariable("scheduleId") Long scheduleId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ScheduleResponse schedule = scheduleService.findById(
                authentication,
                scheduleId,
                request,
                response
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(schedule);
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
