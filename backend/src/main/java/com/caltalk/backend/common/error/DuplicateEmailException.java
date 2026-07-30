package com.caltalk.backend.common.error;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("이미 사용할 수 없는 이메일입니다.");
    }

    public DuplicateEmailException(Throwable cause) {
        super("이미 사용할 수 없는 이메일입니다.", cause);
    }
}
