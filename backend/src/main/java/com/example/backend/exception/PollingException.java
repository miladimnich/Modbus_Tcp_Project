package com.example.backend.exception;

public class PollingException extends RuntimeException {
  public PollingException(String message) {
    super(message);
  }
}