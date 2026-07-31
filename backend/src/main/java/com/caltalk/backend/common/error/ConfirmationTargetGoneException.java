package com.caltalk.backend.common.error;

public class ConfirmationTargetGoneException extends RuntimeException {
    public ConfirmationTargetGoneException() {
        super("The target schedule no longer exists.");
    }
}
