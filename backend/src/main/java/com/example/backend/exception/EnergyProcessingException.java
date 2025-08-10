package com.example.backend.exception;

public class EnergyProcessingException extends RuntimeException {
    public EnergyProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
