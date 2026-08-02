package com.caltalk.backend.common.error;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("이메일 또는 비밀번호를 확인해주세요.");
    }
}
