package com.example.backend.exception;

public class GasProcessingException extends RuntimeException {
    public GasProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
