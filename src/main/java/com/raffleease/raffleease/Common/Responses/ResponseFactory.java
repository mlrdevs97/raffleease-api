package com.raffleease.raffleease.Common.Responses;

import java.util.Map;

public class ResponseFactory {
    public static <T> ApiResponse success(String message) {
        return new SuccessResponse<>(null, message);
    }

    public static <T> ApiResponse success(T data, String message) {
        return new SuccessResponse<>(data, message);
    }

    public static ApiResponse error(String message, int status, String statusText, String code) {
        return new ErrorResponse(message, status, statusText, code);
    }

    public static ApiResponse validationError(String message, int status, String statusText, String code, Map<String, String> errors) {
        return new ValidationErrorResponse(message, status, statusText, code, errors);
    }
}
