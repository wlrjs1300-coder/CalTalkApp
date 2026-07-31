package com.caltalk.backend.common.error;

public class ScheduleVersionConflictException extends RuntimeException {
    public ScheduleVersionConflictException() {
        super("The schedule has been changed. Reload it and try again.");
    }
}
