package com.example.backend.exception;

public class DeviceNorFoundException extends RuntimeException {

  public DeviceNorFoundException(String message) {
    super(message);
  }
}