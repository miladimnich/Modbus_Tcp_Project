package com.example.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Getter
@Setter
public class PollingState {
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isMeasureStarted = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicInteger currentTestStationId = new AtomicInteger(-1);
    private final AtomicReference<String> currentSessionId = new AtomicReference<>(null);


    public void reset() {
        isRunning.set(false);
        isMeasureStarted.set(false);
        stopRequested.set(false);
        currentTestStationId.set(-1);
    }
}
