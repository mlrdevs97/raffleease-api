package com.raffleease.raffleease.Common.Responses;

import lombok.Getter;

@Getter
public class SuccessResponse<T> extends ApiResponse {
    private final T data;

    public SuccessResponse(T data, String message) {
        super(true, message);
        this.data = data;
    }
}
