package com.example.backend.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StartPollingEvent extends ApplicationEvent {
  private final int deviceId;

  public StartPollingEvent(Object source, int deviceId) {
    super(source);
    this.deviceId = deviceId;
  }

}
