package com.example.backend.service.polling;


import com.example.backend.config.PollingState;
import com.example.backend.config.socket.WebSocketHandlerCustom;
import com.example.backend.events.StopPollingEvent;
import com.example.backend.service.chp.ChpService;
import com.example.backend.service.energy.EnergyService;
import com.example.backend.service.gas.GasService;
import com.example.backend.service.heating.HeatingService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


@Slf4j
@Service
@Getter
public class PollingControlService {

    private final EnergyService energyService;
    private final HeatingService heatingService;
    private final ChpService chpService;
    private final GasService gasService;
    private final ModbusPollingService modbusPollingService;
    private final WebSocketHandlerCustom webSocketHandlerCustom;
    private final PollingState pollingState;
    private volatile long endTime = -1L;
    private static final long INITIAL_RESULTS_TIMEOUT_MS = 120_000;


    @Autowired
    public PollingControlService(EnergyService energyService, HeatingService heatingService, ChpService chpService, GasService gasService, ModbusPollingService modbusPollingService, WebSocketHandlerCustom webSocketHandlerCustom, PollingState pollingState) {
        this.energyService = energyService;
        this.heatingService = heatingService;
        this.chpService = chpService;
        this.gasService = gasService;
        this.modbusPollingService = modbusPollingService;
        this.webSocketHandlerCustom = webSocketHandlerCustom;
        this.pollingState = pollingState;
    }


    @EventListener
    public void onStopPollingEvent(StopPollingEvent event) {
        int testStationId = event.getTestStationId();
        this.stopMeasurement(testStationId);
    }


    public void startMeasurement(int testStationId) {

        if (pollingState.getStopRequested().get()) {
            log.warn("Measurement aborted early because stop was already requested for testStationId {}", testStationId);
            return;
        }
        if (!webSocketHandlerCustom.getConnectedSessions().isEmpty() && pollingState.getIsRunning().get()) {
            modbusPollingService.startMeasureTask(testStationId);
        }


        long waitStartTime = System.currentTimeMillis();
        while (!gasService.getIsInitialResultsPopulated().get() && !pollingState.getStopRequested().get() && !Thread.currentThread().isInterrupted()) {
            if (System.currentTimeMillis() - waitStartTime > INITIAL_RESULTS_TIMEOUT_MS) {
                log.error("Timeout while waiting for gas results for testStationId: {}", testStationId);
                String error = "Timeout while waiting for gas results for testStationId: " + testStationId;
                sendError(error);
                pollingState.getStopRequested().set(true);
                modbusPollingService.stopPolling(testStationId);
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.warn("Measurement thread was interrupted", e);
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (!pollingState.getStopRequested().get()) {
            sendInitialResults(testStationId);
        } else {
            log.info("Measurement stopped before initial results were received for testStationId: {}", testStationId);
            String errorMessage = "Measurement stopped before initial results were received for testStationId: " + testStationId;
            sendError(errorMessage);
            modbusPollingService.stopPolling(testStationId);
        }

    }


    public void stopMeasurement(int testStationId) {
        System.out.println("stopMeasurement is called ");

        pollingState.getStopRequested().set(true);

        if (!gasService.getIsInitialResultsPopulated().get() && pollingState.getIsMeasureStarted().get()) {
            log.error("Initial results for gas were not received for the test stationID: {}", testStationId);
            return;
        }

        if (pollingState.getIsRunning().get()) {
            endTime = System.currentTimeMillis();
            modbusPollingService.stopPolling(testStationId);
        }
        if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
            sendLastResults(testStationId);
        }

    }


    private void sendInitialResults(int testStationId) {
        Map<String, Object> combinedResults = new LinkedHashMap<>();
        combinedResults.putAll(energyService.getStartEnergyResults(testStationId));
        combinedResults.putAll(heatingService.getStartHeatingResults(testStationId));
        combinedResults.putAll(gasService.getStartGasResults(testStationId));
        combinedResults.putAll(chpService.getStartChpResults(testStationId));

        long startTime = gasService.getTimestampNow();
        log.info("Start time is: {}", startTime);

        Map<String, Object> response = new HashMap<>();
        response.put("initialData", combinedResults);
        response.put("startTime", startTime);

        if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
            webSocketHandlerCustom.pushDataToClients(response);
        }
    }


    private void sendLastResults(int testStationId) {
        Map<String, Object> combinedResults = new LinkedHashMap<>();
        combinedResults.putAll(energyService.getLastEnergyResults(testStationId));
        combinedResults.putAll(heatingService.getLastHeatingResults(testStationId));
        combinedResults.putAll(gasService.getLastGasResults(testStationId));
        combinedResults.putAll(chpService.getLastChpResults(testStationId));

        Map<String, Object> response = new HashMap<>();
        response.put("lastData", combinedResults);
        response.put("endTime", endTime);

        if (combinedResults.isEmpty()) {
            log.warn("No lastData available to send for testStationId: {}", testStationId);
            return; // Don't push empty data
        }

        if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
            webSocketHandlerCustom.pushDataToClients(response);
        }

    }

    private void sendError(String errorMessage) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", errorMessage);
        if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
            webSocketHandlerCustom.pushDataToClients(errorResponse);
        }
    }


}