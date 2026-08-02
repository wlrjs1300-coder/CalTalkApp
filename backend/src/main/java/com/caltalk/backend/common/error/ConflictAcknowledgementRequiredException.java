package com.caltalk.backend.common.error;

public class ConflictAcknowledgementRequiredException extends RuntimeException {

    public ConflictAcknowledgementRequiredException() {
        super("일정 충돌 확인이 필요합니다.");
    }
}
