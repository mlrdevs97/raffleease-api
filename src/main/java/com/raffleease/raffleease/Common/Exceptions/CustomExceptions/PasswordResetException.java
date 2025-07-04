package com.raffleease.raffleease.Common.Exceptions.CustomExceptions;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

@Getter
public class PasswordResetException extends RuntimeException {
    private final String errorCode;
    
    @Autowired
    public PasswordResetException(String message) {
        super(message);
        this.errorCode = null;
    }
    
    @Autowired
    public PasswordResetException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
