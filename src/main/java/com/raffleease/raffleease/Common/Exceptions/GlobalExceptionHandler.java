package com.raffleease.raffleease.Common.Exceptions;

import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.*;
import com.raffleease.raffleease.Common.Utils.ConstraintNameMapper;
import com.raffleease.raffleease.Common.Responses.ApiResponse;
import com.raffleease.raffleease.Common.Responses.ResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

import static com.raffleease.raffleease.Common.Exceptions.ErrorCodes.VALIDATION_ERROR;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {
    private final ConstraintNameMapper constraintNameMapper;

    private ResponseEntity<ApiResponse> wrapError(Exception ex, HttpStatus status, String code) {
        return ResponseEntity.status(status)
                .body(ResponseFactory.error(ex.getMessage(), status.value(), status.getReasonPhrase(), code));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse> handleNotFoundException(NotFoundException ex) {
        return wrapError(ex, NOT_FOUND, ErrorCodes.NOT_FOUND);
    }

    @ExceptionHandler(CustomMailException.class)
    public ResponseEntity<ApiResponse> handleCustomMailException(CustomMailException ex) {
        return wrapError(ex, INTERNAL_SERVER_ERROR, ErrorCodes.MAIL_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return wrapError(ex, BAD_REQUEST, ErrorCodes.BAD_REQUEST);
    }

    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<ApiResponse> handleEmailVerificationException(EmailVerificationException ex) {
        return wrapError(ex, BAD_REQUEST, ErrorCodes.EMAIL_VERIFICATION_FAILED);
    }

    @ExceptionHandler(UpdateRoleException.class)
    public ResponseEntity<ApiResponse> handleUpdateRoleException(UpdateRoleException ex) {
        String errorCode = ex.getErrorCode() != null ? ex.getErrorCode() : ErrorCodes.INVALID_REQUEST;
        return wrapError(ex, BAD_REQUEST, errorCode);
    }

    @ExceptionHandler(PasswordResetException.class)
    public ResponseEntity<ApiResponse> handlePasswordResetException(PasswordResetException ex) {
        return wrapError(ex, BAD_REQUEST, ErrorCodes.PASSWORD_RESET_FAILED);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiResponse> handleFileStorageException(FileStorageException ex) {
        return wrapError(ex, INTERNAL_SERVER_ERROR, ErrorCodes.FILE_STORAGE_ERROR);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse> handleUserNameNotFoundException(UsernameNotFoundException ex) {
        return wrapError(ex, UNAUTHORIZED, ErrorCodes.USER_NOT_FOUND);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse> handleAuthenticationException(AuthenticationException ex) {
        return wrapError(ex, UNAUTHORIZED, ErrorCodes.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ApiResponse> handleAuthorizationException(AuthorizationException ex) {
        return wrapError(ex, FORBIDDEN, ErrorCodes.ACCESS_DENIED);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse> handleRateLimitExceededException(RateLimitExceededException ex) {
        return wrapError(ex, TOO_MANY_REQUESTS, ErrorCodes.RATE_LIMIT_EXCEEDED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException ex) {
        return wrapError(ex, FORBIDDEN, ErrorCodes.ACCESS_DENIED);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse> handleConflictException(ConflictException ex) {
        return wrapError(ex, CONFLICT, ErrorCodes.CONFLICT);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse> handleBusinessException(BusinessException ex) {
        String errorCode = ex.getErrorCode() != null ? ex.getErrorCode() : ErrorCodes.BUSINESS_ERROR;
        return wrapError(ex, BAD_REQUEST, errorCode);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return wrapError(new IllegalArgumentException("Invalid request payload."), BAD_REQUEST, ErrorCodes.INVALID_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        String message = "Content type '" + ex.getContentType() + "' is not supported. Supported media types are: " + ex.getSupportedMediaTypes();
        return wrapError(new IllegalArgumentException(message), UNSUPPORTED_MEDIA_TYPE, ErrorCodes.INVALID_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGeneralException(Exception ex) {
        return wrapError(new RuntimeException("An unexpected error occurred: " + ex.toString()), INTERNAL_SERVER_ERROR, ErrorCodes.UNEXPECTED_ERROR);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        String message = String.format("Missing required request parameter: '%s'", ex.getParameterName());
        return wrapError(new IllegalArgumentException(message), BAD_REQUEST, ErrorCodes.MISSING_PARAMETER);
    }

    @ExceptionHandler(UniqueConstraintViolationException.class)
    public ResponseEntity<ApiResponse> handleUniqueConstraintViolationException(UniqueConstraintViolationException ex) {
        String field = constraintNameMapper.mapToField(ex.getField());
        Map<String, String> errors = Map.of(field, ErrorCodes.VALUE_ALREADY_EXISTS);

        ApiResponse response = ResponseFactory.validationError(
                "Validation failed",
                CONFLICT.value(),
                CONFLICT.getReasonPhrase(),
                VALIDATION_ERROR,
                errors
        );

        return ResponseEntity.status(CONFLICT).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String fieldName = fieldError.getField();
            String springCode = fieldError.getCode();
            String errorCode = ValidationErrorCodeMapper.toAppCode(springCode);

            errors.put(fieldName, errorCode);
        }

        ApiResponse response = ResponseFactory.validationError(
                "Validation failed",
                BAD_REQUEST.value(),
                BAD_REQUEST.getReasonPhrase(),
                VALIDATION_ERROR,
                errors
        );

        return ResponseEntity.status(BAD_REQUEST).body(response);
    }
}