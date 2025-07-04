package com.raffleease.raffleease.Common.Exceptions.CustomExceptions;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

@EqualsAndHashCode(callSuper = true)
@Getter
public class ConflictException extends RuntimeException {
    @Autowired
    public ConflictException(String message) {
        super(message);
    }
}