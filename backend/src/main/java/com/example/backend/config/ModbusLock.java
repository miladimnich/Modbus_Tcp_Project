package com.example.backend.config;

public class ModbusLock {
  private final Object lock = new Object();

  public Object getLock() {
    return lock;
  }
}
