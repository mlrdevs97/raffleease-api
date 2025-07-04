package com.raffleease.raffleease.Common.Exceptions.CustomExceptions;

import lombok.Getter;

@Getter
public class CustomMailException extends RuntimeException {
    public CustomMailException(String msg) { super(msg); }
}
