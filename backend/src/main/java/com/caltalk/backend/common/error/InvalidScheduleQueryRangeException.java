package com.caltalk.backend.common.error;

public class InvalidScheduleQueryRangeException extends RuntimeException {

    public InvalidScheduleQueryRangeException() {
        super("조회 종료 시각은 조회 시작 시각보다 늦어야 합니다.");
    }
}
