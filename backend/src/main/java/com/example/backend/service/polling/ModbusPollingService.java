package com.example.backend.service.polling;


import com.example.backend.config.MeasurementSessionRegistry;
import com.example.backend.config.PollingState;
import com.example.backend.config.socket.WebSocketHandlerCustom;
import com.example.backend.config.threading.ScheduledTaskRegistry;
import com.example.backend.events.StartPollingEvent;
import com.example.backend.exception.*;
import com.example.backend.service.chp.ChpService;
import com.example.backend.service.energy.EnergyService;
import com.example.backend.service.gas.GasService;
import com.example.backend.service.heating.HeatingService;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Slf4j
@Service
@Getter
public class ModbusPollingService {

    private final EnergyService energyService;
    private final HeatingService heatingService;
    private final GasService gasService;
    private final ChpService chpService;
    private final MeasurementSessionRegistry measurementSessionRegistry;
    private final WebSocketHandlerCustom webSocketHandlerCustom;
    private final ScheduledExecutorService executorService;
    private final ScheduledTaskRegistry scheduledTaskRegistry;
    private final PollingState pollingState;


    @Autowired
    public ModbusPollingService(EnergyService energyService, HeatingService heatingService,
                                GasService gasService, ChpService chpService, MeasurementSessionRegistry measurementSessionRegistry, WebSocketHandlerCustom webSocketHandlerCustom, ScheduledExecutorService executorService, ScheduledTaskRegistry scheduledTaskRegistry, PollingState pollingState) {
        this.energyService = energyService;
        this.heatingService = heatingService;
        this.gasService = gasService;
        this.chpService = chpService;
        this.measurementSessionRegistry = measurementSessionRegistry;
        this.webSocketHandlerCustom = webSocketHandlerCustom;
        this.executorService = executorService;
        this.scheduledTaskRegistry = scheduledTaskRegistry;
        this.pollingState = pollingState;
    }

    @EventListener
    public void handleStartPollingEvent(StartPollingEvent event) {
        startPolling(event.getTestStationId());
    }


    @Synchronized
    public void startPolling(int testStationId) {
        if (webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
            throw new PollingException("No active WebSocket connection.");
        }
        int currentStationId = pollingState.getCurrentTestStationId().get();
        boolean isPolling = pollingState.getIsRunning().get();

        // If switching stations while polling
        if (isPolling && currentStationId != testStationId) {
            log.info("Switching from test station {} to {}", currentStationId, testStationId);
            stopPolling(currentStationId);
        }
        // If polling is still marked as running (race condition, or not stopped correctly)
        if (pollingState.getIsRunning().get()) {
            log.warn("Polling still marked as running. Forcing stop.");
            stopPolling(currentStationId);
        }
        // Now it's safe to start
        clearPreviousResults();
        pollingState.getCurrentTestStationId().set(testStationId);
        pollingState.getIsRunning().set(true);

        webSocketHandlerCustom.startWebSocketUpdateTask(testStationId);  // Start sending updates

        scheduledTaskRegistry.register(testStationId, executorService.scheduleWithFixedDelay(() -> {
            synchronized (this) { // Prevent overlapping executions
                try {
                    energyService.processEnergyData(testStationId);
                    log.info("Completed processEnergyData for test station {}", testStationId);

                    heatingService.processHeatingData(testStationId);
                    log.info("Completed processHeatingData for test station {}", testStationId);
                } catch (EnergyProcessingException | HeatingProcessingException e) {
                    log.error("Stopping polling due to critical failure for test station {}: {}", testStationId, e.getMessage(), e);
                    stopPolling(testStationId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Thread interrupted during data processing for test station {}", testStationId, e);
                    stopPolling(testStationId);
                } catch (Exception e) {
                    log.error("Unexpected error in scheduled data processing for test station {}", testStationId, e);
                    stopPolling(testStationId);
                }
            }
        }, 0, 1, TimeUnit.SECONDS));


        scheduledTaskRegistry.register(testStationId, executorService.scheduleWithFixedDelay(() -> {
            try {
                chpService.processDataChp(testStationId);
                log.info("Completed processChpData for test station {}", testStationId);
            } catch (ChpProcessingException e) {
                log.error("Stopping polling due to critical failure for test station {}: {}", testStationId, e.getMessage(), e);
                stopPolling(testStationId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Thread interrupted during data processing for test station {}", testStationId, e);
                stopPolling(testStationId);
            } catch (Exception e) {
                log.error("Unexpected error in scheduled data processing for test station {}", testStationId, e);
                stopPolling(testStationId);
            }

        }, 0, 1, TimeUnit.SECONDS));

        scheduledTaskRegistry.register(testStationId, executorService.scheduleWithFixedDelay(() -> {
            try {
                gasService.processGasData(testStationId);
                log.info("Completed processGasData for device {}", testStationId);
            } catch (GasProcessingException e) {
                log.error("Stopping polling due to critical failure for device {}: {}", testStationId, e.getMessage(), e);
                stopPolling(testStationId);
            } catch (InterruptedException e) {
                log.warn("Thread interrupted during data processing for device {}", testStationId, e);
                stopPolling(testStationId);
            } catch (Exception e) {
                log.error("Unexpected error in scheduled gas data processing for device {}", testStationId, e);
                stopPolling(testStationId);
            }
        }, 0, 1, TimeUnit.SECONDS));
    }


    public synchronized void startMeasureTask(int testStationId) {
        System.out.println("Calling startMeasureTask for device " + testStationId + ". isMeasureStarted = " + pollingState.getIsMeasureStarted().get());
        if (pollingState.getStopRequested().get()) {
            log.warn("Measurement task scheduling aborted because stop was requested for testStationId {}", testStationId);
            return;
        }

        if (pollingState.getIsMeasureStarted().get()) {
            System.out.println("Measure task is already started for device " + testStationId);
            return; // Prevent starting measure task again if already running
        }

        pollingState.getIsMeasureStarted().set(true);  // Mark measure task as started

        scheduledTaskRegistry.register(testStationId, executorService.scheduleWithFixedDelay(() -> {
            try {
                gasService.calculateAndPushMeterDifference(testStationId);
                log.info("Completed calculateMeterDifference for device {}", testStationId);
            } catch (InterruptedException e) {
                log.warn("Thread interrupted during data processing for device {}", testStationId, e);
                stopPolling(testStationId);
            } catch (Exception e) {
                log.error("Unexpected error in scheduled gas data processing for device {}", testStationId, e);
                stopPolling(testStationId);
            }
        }, 0, 1, TimeUnit.SECONDS));

        scheduledTaskRegistry.register(testStationId, executorService.scheduleWithFixedDelay(() -> {

            try {
                energyService.calculateAndPushEnergyDifference(testStationId);
                log.info("Completed calculateEnergyDifference for device {}", testStationId);
            } catch (InterruptedException e) {
                log.warn("Thread interrupted during data processing for device {}", testStationId, e);
                stopPolling(testStationId);
            } catch (Exception e) {
                log.error("Unexpected error in scheduled gas data processing for device {}", testStationId, e);
                stopPolling(testStationId);
            }
        }, 0, 1, TimeUnit.SECONDS));

        scheduledTaskRegistry.register(testStationId, executorService.scheduleWithFixedDelay(() -> {
            try {
                heatingService.calculateAndPushHeatingDifference(testStationId);
                log.info("Completed calculateHeatingDifference for device {}", testStationId);
            } catch (InterruptedException e) {
                log.warn("Thread interrupted during data processing for device {}", testStationId, e);
                stopPolling(testStationId);
            } catch (Exception e) {
                log.error("Unexpected error in scheduled gas data processing for device {}", testStationId, e);
                stopPolling(testStationId);
            }
        }, 0, 1, TimeUnit.SECONDS));
    }


    public synchronized void stopPolling(int testStationId) {
        log.info("stopPolling called for test station {}", testStationId);
        if (!pollingState.getIsRunning().getAndSet(false)) {
            log.info("Polling already stopped for test station {}", testStationId);
            if (pollingState.getIsMeasureStarted().getAndSet(false)) {
                log.info("Measure task stopped during stopPolling for test station {}", testStationId);
            }
            return;
        }
        pollingState.getIsRunning().set(false);
        pollingState.getIsMeasureStarted().set(false);
        scheduledTaskRegistry.cancelAll(testStationId);
        webSocketHandlerCustom.flushPendingDataToOpenSessionsBeforeShutdown(testStationId);
        log.info("after cancelling tasks UpdateQueue has size {}", webSocketHandlerCustom.getUpdateQueue().size());
    }

    private void clearPreviousResults() {
        energyService.clearEnergyResults();
        heatingService.clearHeatingResults();
        gasService.clearGasResults();
        chpService.clearChpResults();
        measurementSessionRegistry.setSerialNumber(null);
        pollingState.reset();
    }
}
