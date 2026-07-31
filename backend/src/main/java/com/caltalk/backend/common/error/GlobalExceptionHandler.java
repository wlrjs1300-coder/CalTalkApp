package com.caltalk.backend.common.error;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.caltalk.backend.schedule.ScheduleConflictResponse;

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception
    ) {
        return errorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "VALIDATION_ERROR",
                VALIDATION_MESSAGE,
                List.of()
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

    @ExceptionHandler(InvalidTimezoneException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTimezone(InvalidTimezoneException exception) {
        FieldErrorResponse fieldError = new FieldErrorResponse(
                "timezone",
                "INVALID_TIMEZONE",
                exception.getMessage()
        );
        return errorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "VALIDATION_ERROR",
                VALIDATION_MESSAGE,
                List.of(fieldError)
        );
    }

    @ExceptionHandler(InvalidScheduleTimeRangeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidScheduleTimeRange(
            InvalidScheduleTimeRangeException exception
    ) {
        return errorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "VALIDATION_ERROR",
                VALIDATION_MESSAGE,
                List.of(new FieldErrorResponse(
                        "endAt",
                        "INVALID_TIME_RANGE",
                        exception.getMessage()
                ))
        );
    }

    @ExceptionHandler(InvalidScheduleQueryRangeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidScheduleQueryRange(
            InvalidScheduleQueryRangeException exception
    ) {
        return errorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "VALIDATION_ERROR",
                VALIDATION_MESSAGE,
                List.of(new FieldErrorResponse(
                        "to",
                        "INVALID_TIME_RANGE",
                        exception.getMessage()
                ))
        );
    }

    @ExceptionHandler(InvalidScheduleUpdateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidScheduleUpdate(
            InvalidScheduleUpdateException exception
    ) {
        return errorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "VALIDATION_ERROR",
                VALIDATION_MESSAGE,
                List.of(new FieldErrorResponse(
                        exception.getField(), exception.getErrorCode(), exception.getMessage()))
        );
    }

    @ExceptionHandler(ScheduleVersionConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleScheduleVersionConflict(
            ScheduleVersionConflictException exception
    ) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "SCHEDULE_VERSION_CONFLICT",
                exception.getMessage(),
                List.of()
        );
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException exception
    ) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "SCHEDULE_VERSION_CONFLICT",
                "The schedule has been changed. Reload it and try again.",
                List.of()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException exception
    ) {
        return errorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "VALIDATION_ERROR",
                VALIDATION_MESSAGE,
                List.of(new FieldErrorResponse(
                        exception.getParameterName(),
                        "REQUIRED",
                        "필수 입력값입니다."
                ))
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return errorResponse(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "VALIDATION_ERROR",
                VALIDATION_MESSAGE,
                List.of(new FieldErrorResponse(
                        exception.getName(),
                        "INVALID_FORMAT",
                        "올바른 형식으로 입력해주세요."
                ))
        );
    }

    @ExceptionHandler(ScheduleNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleScheduleNotFound(
            ScheduleNotFoundException exception
    ) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                "SCHEDULE_NOT_FOUND",
                exception.getMessage(),
                List.of()
        );
    }

    @ExceptionHandler(ScheduleConflictException.class)
    public ResponseEntity<ScheduleConflictResponse> handleScheduleConflict(
            ScheduleConflictException exception
    ) {
        return conflictResponse(
                "SCHEDULE_CONFLICT",
                exception.getMessage(),
                exception.getConfirmationId(),
                exception.getConflicts()
        );
    }

    @ExceptionHandler(ConfirmationSupersededException.class)
    public ResponseEntity<ScheduleConflictResponse> handleConfirmationSuperseded(
            ConfirmationSupersededException exception
    ) {
        return conflictResponse(
                "CONFIRMATION_SUPERSEDED",
                exception.getMessage(),
                exception.getConfirmationId(),
                exception.getConflicts()
        );
    }

    @ExceptionHandler(ConfirmationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleConfirmationNotFound(
            ConfirmationNotFoundException exception
    ) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                "CONFIRMATION_NOT_FOUND",
                exception.getMessage(),
                List.of()
        );
    }

    @ExceptionHandler(ConfirmationTargetGoneException.class)
    public ResponseEntity<ApiErrorResponse> handleConfirmationTargetGone(
            ConfirmationTargetGoneException exception
    ) {
        return errorResponse(
                HttpStatus.NOT_FOUND,
                "CONFIRMATION_TARGET_GONE",
                exception.getMessage(),
                List.of()
        );
    }

    @ExceptionHandler(ConfirmationForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleConfirmationForbidden(
            ConfirmationForbiddenException exception
    ) {
        return errorResponse(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                exception.getMessage(),
                List.of()
        );
    }

    @ExceptionHandler(ConflictAcknowledgementRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleConflictAcknowledgementRequired(
            ConflictAcknowledgementRequiredException exception
    ) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "CONFLICT_ACKNOWLEDGEMENT_REQUIRED",
                exception.getMessage(),
                List.of()
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

    @ExceptionHandler(UnauthorizedCurrentUserException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorizedCurrentUser(
            UnauthorizedCurrentUserException exception
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                exception.getMessage(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .cacheControl(CacheControl.noStore())
                .body(response);
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

    private ResponseEntity<ScheduleConflictResponse> conflictResponse(
            String code,
            String message,
            Long confirmationId,
            List<com.caltalk.backend.schedule.ConflictingScheduleResponse> conflicts
    ) {
        ScheduleConflictResponse response = new ScheduleConflictResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                code,
                message,
                List.of(),
                confirmationId,
                conflicts
        );
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    private FieldErrorResponse toFieldError(FieldError error) {
        String field = error.getField();
        String validationCode = error.getCode();

        if ("timezone".equals(field)) {
            return new FieldErrorResponse(
                    field,
                    "INVALID_TIMEZONE",
                    "올바른 시간대를 선택해주세요."
            );
        }
        if (("title".equals(field) || "location".equals(field))
                && "Size".equals(validationCode)) {
            return new FieldErrorResponse(field, "MAX_LENGTH", "200자 이하여야 합니다.");
        }
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
