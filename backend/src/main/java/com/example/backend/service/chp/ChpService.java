package com.example.backend.service.chp;


import com.example.backend.config.socket.WebSocketHandlerCustom;
import com.example.backend.enums.ChpCalculationType;
import com.example.backend.enums.SubDeviceType;
import com.example.backend.exception.ChpProcessingException;
import com.example.backend.exception.ModbusDeviceException;
import com.example.backend.models.ModbusDevice;
import com.example.backend.models.SubDevice;
import com.example.backend.models.TestStation;
import com.example.backend.service.modbus.ModbusBitwiseService;
import com.example.backend.service.teststation.TestStationService;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.messaging.TimeoutException;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Getter
public class ChpService {
    private final Map<String, Object> initialChpResults = new ConcurrentHashMap<>();
    private final Map<String, Object> currentChpResults = new ConcurrentHashMap<>();
    private final Map<String, Object> lastChpResults = new ConcurrentHashMap<>();

    private final ModbusBitwiseService modbusBitwiseService;
    private final TestStationService testStationService;
    private final WebSocketHandlerCustom webSocketHandlerCustom;

    @Autowired
    public ChpService(TestStationService testStationService, WebSocketHandlerCustom webSocketHandlerCustom, ModbusBitwiseService modbusBitwiseService) {
        this.testStationService = testStationService;
        this.webSocketHandlerCustom = webSocketHandlerCustom;
        this.modbusBitwiseService = modbusBitwiseService;
    }

    private void formatAndProcess(int testStationID, ChpCalculationType type, long result) {
        double value = (double) result / 10;
        String formattedResult = String.format(Locale.US, "%.2f", value);
        processAndPushCurrentResults(testStationID, type.name(), formattedResult);
    }

    public void processDataChp(int testStationId) throws InterruptedException {
        log.info("Starting chp data processing for TestStation ID: {}", testStationId);
        TestStation testStation = testStationService.getTestStationById(testStationId);
        if (testStation == null) {
            log.warn("No TestStation found for ID: {}", testStationId);
            return;
        }
        for (ModbusDevice modbusDevice : testStation.getModbusDevices()) {
            for (SubDevice subDevice : modbusDevice.getSubDevices()) {
                if (!subDevice.getType().equals(SubDeviceType.CHP)) {
                    log.debug("Skipping SubDevice with slaveId={}, startAddress={}, type={}",
                            subDevice.getSlaveId(), subDevice.getStartAddress(), subDevice.getType());
                    continue;
                }
                List<ChpCalculationType> chpCalculationTypes = subDevice.getChpCalculationTypes();
                if (chpCalculationTypes == null) continue;
                for (ChpCalculationType chpCalculationType : chpCalculationTypes) {
                    int startAddress = chpCalculationType.getStartAddress(subDevice);
                    try {
                        log.debug("Calculating {} for SubDevice ID: {} at address {}", chpCalculationType.name(), subDevice.getType(), startAddress);
                        long result = modbusBitwiseService.bitwiseShiftCalculation(
                                testStationId, startAddress, modbusDevice, subDevice);
                        log.info("result for start address {} is {}", startAddress, result);

                        switch (chpCalculationType) {
                            case OPERATING_HOURS, START_COUNT ->
                                    processAndPushCurrentResults(testStationId, chpCalculationType.name(), result);
                            case EXHAUST_TEMPERATURE, HEATING_WATER_FLOW, HEATING_WATER_RETURN,
                                 ENGINE_COOLANT_RETURN, ENGINE_COOLANT_FLOW,
                                 CONTROL_CABINET, HOUSING, GENERATOR_WINDING,
                                 ENGINE_OIL, ENGINE_COOLANT ->
                                    formatAndProcess(testStationId, chpCalculationType, result);
                            default -> log.warn("Unhandled ChpCalculationType: {}", chpCalculationType);
                        }
                    } catch (InterruptedException ie) {
                        log.warn("Thread interrupted during chp data processing for device {}", testStationId, ie);
                        Thread.currentThread().interrupt(); // preserve interrupt status
                        throw ie; // propagate InterruptedException
                    } catch (ModbusDeviceException | WaitingRoomException | TimeoutException |
                             IllegalStateException | ModbusTransportException ex) {
                        log.error("Exception [{} - {}]: {}", chpCalculationType.name(), testStationId, ex.getMessage(), ex);
                        throw new ChpProcessingException("Critical failure in chp processing", ex);
                    } catch (Exception ex) {
                        log.error("Unexpected exception [{} - {}]: {}", chpCalculationType.name(), testStationId, ex.getMessage(), ex);
                        Thread.currentThread().interrupt();
                        throw new InterruptedException("Thread interrupted due to exception: " + ex.getMessage());
                    }
                }
            }
        }
    }

    private void processAndPushCurrentResults(int testStationID, String key, Object value) {
        log.debug("Processing value: Key = {}, Value = {}, testStationId = {}", key, value, testStationID);
        try {
            Object lastValue = lastChpResults.get(key); // at the beginning its null after value for the specific key
            log.debug("🔎 Previous value for key '{}': {}", key, lastValue);
            boolean valueChanged = (lastValue == null || !lastValue.equals(value));

            log.debug("🗂 Before update - currentChpResults[{}]: {}", key, currentChpResults.get(key));
            currentChpResults.put(key, value);
            log.debug("📌 After update - currentChpResults[{}]: {}", key, currentChpResults.get(key));
            if (valueChanged) { // true means lastEnergyResults empty or the value differs
                lastChpResults.put(key, value);
                Map<String, Object> update = new LinkedHashMap<>();
                update.put(key, value);
                update.put("testStationId", testStationID);
                log.debug("Value changed, enqueuing update: {}", update);

                if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
                    webSocketHandlerCustom.enqueueUpdate(update);
                }
            } else {
                log.debug("Value did not change, skipping push for key: {}, testStationID: {}", key, testStationID);
            }
        } catch (Exception e) {
            log.error("Unexpected error in energy  processAndPush for key: {}, testStationID: {} - {}", key, testStationID, e.getMessage(), e);
        }

    }


    public synchronized Map<String, Object> getStartChpResults(int testStationID) {
        log.debug("Saving current chp results as start results for testStationID: {}", testStationID);
        initialChpResults.putAll(currentChpResults);
        return new HashMap<>(initialChpResults);
    }

    public Map<String, Object> getLastChpResults(int testStationID) {
        log.debug("Fetching last chp results for testStationID: {}", testStationID);
        return new HashMap<>(lastChpResults);
    }

    public synchronized void clearChpResults() {
        initialChpResults.clear();
        currentChpResults.clear();
        lastChpResults.clear();
    }
}
