package com.raffleease.raffleease.Common.Responses;

import lombok.Getter;

@Getter
public class ErrorResponse extends ApiResponse {
    private final int statusCode;
    private final String statusText;
    private final String code;

    public ErrorResponse(String message, int statusCode, String statusText, String code) {
        super(false, message);
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.code = code;
    }
}
