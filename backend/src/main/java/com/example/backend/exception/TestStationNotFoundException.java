package com.example.backend.exception;

public class TestStationNotFoundException extends RuntimeException {
    public TestStationNotFoundException(String message) {
        super(message);
    }
}
