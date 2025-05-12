package com.example.backend.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StopPollingEvent extends ApplicationEvent {
  private final int deviceId;

  public StopPollingEvent(Object source, int deviceId) {
    super(source);
    this.deviceId = deviceId;
  }
}