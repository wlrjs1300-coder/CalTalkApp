package com.caltalk.backend.common.error;

public class ConfirmationForbiddenException extends RuntimeException {

    public ConfirmationForbiddenException() {
        super("요청을 처리할 권한이 없습니다.");
    }
}
