package com.example.crackcs.common.web;

import com.example.crackcs.common.web.response.ApiErrorResponse;
import com.example.crackcs.exception.DuplicateAuthAccountException;
import com.example.crackcs.exception.InvalidCredentialsException;
import com.example.crackcs.exception.MemberNotFoundException;
import com.example.crackcs.exception.TooManyLoginAttemptsException;
import com.example.crackcs.exception.QuestionNotFoundException;
import com.example.crackcs.exception.TopicNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<ApiErrorResponse> handleTooManyLoginAttempts(TooManyLoginAttemptsException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                "TOO_MANY_LOGIN_ATTEMPTS",
                exception.getMessage(),
                List.of(),
                UUID.randomUUID().toString()
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", Long.toString(exception.getRetryAfterSeconds()))
                .body(response);
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMemberNotFound(MemberNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", exception.getMessage(), List.of());
    }

    @ExceptionHandler(DuplicateAuthAccountException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateAuthAccount(DuplicateAuthAccountException exception) {
        return error(HttpStatus.CONFLICT, "DUPLICATE_AUTH_ACCOUNT", exception.getMessage(), List.of());
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleQuestionNotFound(QuestionNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler(TopicNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTopicNotFound(TopicNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiErrorResponse.FieldErrorResponse> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiErrorResponse.FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();

        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "요청 값이 올바르지 않습니다.",
                fieldErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<ApiErrorResponse.FieldErrorResponse> fieldErrors = exception.getConstraintViolations()
                .stream()
                .map(violation -> new ApiErrorResponse.FieldErrorResponse(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "요청 값이 올바르지 않습니다.",
                fieldErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage() {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "요청 본문을 읽을 수 없습니다.",
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        ApiErrorResponse.FieldErrorResponse fieldError = new ApiErrorResponse.FieldErrorResponse(
                exception.getName(),
                "지원하지 않는 값입니다."
        );
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "요청 파라미터를 읽을 수 없습니다.",
                List.of(fieldError)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException() {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "서버에서 요청을 처리하는 중 오류가 발생했습니다.",
                List.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message, List<ApiErrorResponse.FieldErrorResponse> fieldErrors) {
        ApiErrorResponse response = new ApiErrorResponse(code, message, fieldErrors, UUID.randomUUID().toString());
        return ResponseEntity.status(status).body(response);
    }
}
