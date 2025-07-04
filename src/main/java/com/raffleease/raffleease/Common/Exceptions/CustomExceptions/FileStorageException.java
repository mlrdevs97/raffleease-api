package com.raffleease.raffleease.Common.Exceptions.CustomExceptions;

import lombok.Getter;

@Getter
public class FileStorageException extends RuntimeException {
    public FileStorageException(String msg) { super(msg); }
}
