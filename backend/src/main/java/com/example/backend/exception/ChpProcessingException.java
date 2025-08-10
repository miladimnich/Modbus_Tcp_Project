package com.example.backend.exception;

public class ChpProcessingException extends RuntimeException {
    public ChpProcessingException(String message, Throwable cause) {
        super(message,cause);
    }
}
