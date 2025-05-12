package com.example.backend.service;

import org.springframework.context.ApplicationEvent;

public class StopPollingEvent extends ApplicationEvent {
  private final int deviceId;

  public StopPollingEvent(Object source, int deviceId) {
    super(source);
    this.deviceId = deviceId;
  }

  public int getDeviceId() {
    return deviceId;
  }
}
