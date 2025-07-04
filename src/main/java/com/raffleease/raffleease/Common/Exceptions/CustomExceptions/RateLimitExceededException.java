package com.raffleease.raffleease.Common.Exceptions.CustomExceptions;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
} 