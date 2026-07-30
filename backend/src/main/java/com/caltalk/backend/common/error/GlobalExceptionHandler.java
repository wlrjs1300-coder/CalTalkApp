package com.caltalk.backend.common.error;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_MESSAGE = "입력한 내용을 다시 확인해주세요.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .sorted(Comparator.comparing(FieldErrorResponse::field))
                .toList();

        return errorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "VALIDATION_ERROR",
                VALIDATION_MESSAGE,
                fieldErrors
        );
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handlePasswordMismatch(PasswordMismatchException exception) {
        FieldErrorResponse fieldError = new FieldErrorResponse(
                exception.getField(),
                exception.getErrorCode(),
                exception.getMessage()
        );
        return errorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "VALIDATION_ERROR",
                VALIDATION_MESSAGE,
                List.of(fieldError)
        );
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(DuplicateEmailException exception) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "DUPLICATE_EMAIL",
                exception.getMessage(),
                List.of()
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
        return errorResponse(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                exception.getMessage(),
                List.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> errorResponse(
            HttpStatus status,
            String code,
            String message,
            List<FieldErrorResponse> fieldErrors
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                fieldErrors
        );
        return ResponseEntity.status(status).body(response);
    }

    private FieldErrorResponse toFieldError(FieldError error) {
        String field = error.getField();
        String validationCode = error.getCode();

        if ("password".equals(field)
                && "NotBlank".equals(validationCode)
                && error.getRejectedValue() instanceof String rejectedValue
                && !rejectedValue.isEmpty()) {
            return new FieldErrorResponse(
                    field,
                    "INVALID_PASSWORD",
                    "비밀번호는 공백만으로 구성할 수 없습니다."
            );
        }
        if ("email".equals(field) && "Email".equals(validationCode)) {
            return new FieldErrorResponse(field, "INVALID_EMAIL", "올바른 이메일 형식이 아닙니다.");
        }
        if ("email".equals(field) && "Size".equals(validationCode)) {
            return new FieldErrorResponse(field, "EMAIL_TOO_LONG", "이메일은 254자 이하여야 합니다.");
        }
        if ("password".equals(field) && "Size".equals(validationCode)) {
            return new FieldErrorResponse(field, "INVALID_PASSWORD_LENGTH", "비밀번호는 8자 이상 64자 이하여야 합니다.");
        }
        if ("passwordConfirmation".equals(field) && "Size".equals(validationCode)) {
            return new FieldErrorResponse(
                    field,
                    "INVALID_PASSWORD_LENGTH",
                    "비밀번호 확인은 8자 이상 64자 이하여야 합니다."
            );
        }
        return new FieldErrorResponse(field, "REQUIRED", "필수 입력값입니다.");
    }
}
