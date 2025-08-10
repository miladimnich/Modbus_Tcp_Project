package com.example.backend.exception;

public class HeatingProcessingException extends RuntimeException {
    public HeatingProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
