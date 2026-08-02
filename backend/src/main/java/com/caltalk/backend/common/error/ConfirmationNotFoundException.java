package com.caltalk.backend.common.error;

public class ConfirmationNotFoundException extends RuntimeException {

    public ConfirmationNotFoundException() {
        super("만료됐거나 이미 처리된 요청입니다.");
    }
}
