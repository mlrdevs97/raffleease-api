package com.raffleease.raffleease.Common.Exceptions.CustomExceptions;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

@EqualsAndHashCode(callSuper = true)
@Getter
public class NotFoundException extends RuntimeException{
    @Autowired
    public NotFoundException(String message) {
        super(message);
    }
}