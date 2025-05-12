package com.example.backend.exception;

public class ModbusIOException extends RuntimeException {
  public ModbusIOException(String message) {
    super(message);
  }
}