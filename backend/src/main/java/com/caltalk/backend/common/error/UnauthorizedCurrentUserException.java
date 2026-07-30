package com.caltalk.backend.common.error;

public class UnauthorizedCurrentUserException extends RuntimeException {

    public UnauthorizedCurrentUserException() {
        super("인증이 필요합니다.");
    }
}
