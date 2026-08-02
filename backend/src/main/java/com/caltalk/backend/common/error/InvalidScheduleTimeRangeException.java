package com.caltalk.backend.common.error;

public class InvalidScheduleTimeRangeException extends RuntimeException {

    public InvalidScheduleTimeRangeException() {
        super("종료 시각은 시작 시각보다 늦어야 합니다.");
    }
}
