package com.example.backend.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GasCalculationCompleteEvent extends ApplicationEvent {
  private final int deviceId;

  public GasCalculationCompleteEvent(Object source,  int deviceId) {
    super(source);
    this.deviceId = deviceId;
  }
}
