package com.raffleease.raffleease.Common.Exceptions.CustomExceptions;

import lombok.Getter;

@Getter
public class UniqueConstraintViolationException extends RuntimeException {
    private final String field;

    public UniqueConstraintViolationException(String field, String message) {
        super(message);
        this.field = field;
    }
} 