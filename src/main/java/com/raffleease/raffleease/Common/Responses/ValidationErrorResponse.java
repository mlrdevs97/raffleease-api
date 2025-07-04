package com.raffleease.raffleease.Common.Responses;

import lombok.Getter;

import java.util.Map;

@Getter
public class ValidationErrorResponse extends ErrorResponse {
    private final Map<String, String> errors;

    public ValidationErrorResponse(String message, int statusCode, String statusText, String code, Map<String, String> errors) {
        super(message, statusCode, statusText, code);
        this.errors = errors;
    }
}
