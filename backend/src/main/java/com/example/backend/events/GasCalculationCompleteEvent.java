package com.example.backend.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GasCalculationCompleteEvent extends ApplicationEvent {
    private final int testStationId;

    public GasCalculationCompleteEvent(Object source,  int testStationId) {
        super(source);
        this.testStationId = testStationId;
    }
}
