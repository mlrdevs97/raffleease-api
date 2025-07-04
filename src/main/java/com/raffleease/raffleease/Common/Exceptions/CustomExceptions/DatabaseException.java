package com.raffleease.raffleease.Common.Exceptions.CustomExceptions;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

@Getter
public class DatabaseException extends RuntimeException {
    @Autowired
    public DatabaseException(String message) {
        super(message);
    }
}