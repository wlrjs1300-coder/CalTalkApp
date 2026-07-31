package com.caltalk.backend.common.error;

public class InvalidTimezoneException extends RuntimeException {

    public InvalidTimezoneException() {
        super("올바른 시간대를 선택해주세요.");
    }
}
