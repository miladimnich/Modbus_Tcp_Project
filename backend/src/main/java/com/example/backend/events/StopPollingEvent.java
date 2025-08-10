package com.example.backend.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StopPollingEvent extends ApplicationEvent {
    private final int testStationId;

    public StopPollingEvent(Object source, int testStationId) {
        super(source);
        this.testStationId = testStationId;
    }
}
