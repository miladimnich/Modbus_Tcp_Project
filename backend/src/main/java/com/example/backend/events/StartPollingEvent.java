package com.example.backend.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StartPollingEvent extends ApplicationEvent {
    private final int testStationId;


    public StartPollingEvent(Object source, int testStationId) {
        super(source);
        this.testStationId = testStationId;
    }
}
