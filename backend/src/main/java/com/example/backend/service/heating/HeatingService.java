package com.example.backend.service.heating;

import com.example.backend.config.socket.WebSocketHandlerCustom;
import com.example.backend.enums.HeatingCalculationType;
import com.example.backend.enums.SubDeviceType;
import com.example.backend.exception.HeatingProcessingException;
import com.example.backend.exception.ModbusDeviceException;
import com.example.backend.models.ModbusDevice;
import com.example.backend.models.SubDevice;
import com.example.backend.models.TestStation;
import com.example.backend.service.gas.GasService;
import com.example.backend.service.modbus.ModbusBitwiseService;
import com.example.backend.service.teststation.TestStationService;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.messaging.TimeoutException;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.backend.enums.HeatingCalculationType.GENERATED_ENERGY_HEATING;


@Slf4j
@Service
@Getter
@Setter
public class HeatingService {
  private final Map<String, Object> initialHeatingResults = new ConcurrentHashMap<>();
  private final Map<String, Object> currentHeatingResults = new ConcurrentHashMap<>();
  private final Map<String, Object> lastHeatingResults = new ConcurrentHashMap<>();
  private final AtomicReference<Double> heatingDifference = new AtomicReference<>(Double.NaN);

  private final ModbusBitwiseService modbusBitwiseService;
  private final TestStationService testStationService;
  private final WebSocketHandlerCustom webSocketHandlerCustom;
  private final GasService gasService;

  @Autowired
  public HeatingService(WebSocketHandlerCustom webSocketHandlerCustom, ModbusBitwiseService modbusBitwiseService, TestStationService testStationService, GasService gasService) {
    this.webSocketHandlerCustom = webSocketHandlerCustom;
    this.modbusBitwiseService = modbusBitwiseService;
    this.testStationService = testStationService;
    this.gasService = gasService;
  }

  private void formatAndProcess(int testStationId, HeatingCalculationType type, long rawValue, double divisor) {
    double value = (double) rawValue / divisor;
    String formattedResult = String.format(Locale.US, "%.2f", value);
    processAndPushCurrentResults(testStationId, type.name(), formattedResult);
  }


  public void processHeatingData(int testStationId) throws InterruptedException {
    if (hasHeatingSubDevice(testStationId)) {
      log.info("No HEATING subdevice found for testStationId: {}. Skipping processHeatingData.", testStationId);
      return;
    }

    log.info("Starting heating data processing for TestStation ID: {}", testStationId);
    TestStation testStation = testStationService.getTestStationById(testStationId);
    if (testStation == null) {
      log.warn("No TestStation found for testStationId: {}", testStationId);
      return;
    }

    for (ModbusDevice modbusDevice : testStation.getModbusDevices()) {
      for (SubDevice subDevice : modbusDevice.getSubDevices()) {
        if (!SubDeviceType.HEATING.equals(subDevice.getType())) {
          log.debug("Skipping SubDevice of type {}", subDevice.getType());
          continue;
        }

        List<HeatingCalculationType> heatingCalculationTypes = subDevice.getHeatingCalculationTypes();
        if (heatingCalculationTypes == null) continue;
        for (HeatingCalculationType heatingCalculationType : heatingCalculationTypes) {
          int startAddress = heatingCalculationType.getStartAddress(subDevice);
          try {
            log.debug("Calculating {} for SubDevice ID: {} at address {}", heatingCalculationType.name(), subDevice.getType(), startAddress);
            long result = modbusBitwiseService.bitwiseShiftCalculation(testStationId, startAddress, modbusDevice, subDevice);
            log.info("result for start address {} is {}", startAddress, result);
            switch (heatingCalculationType) {
              case TEMPERATURE_DIFFERENCE ->
                      formatAndProcess(testStationId, heatingCalculationType, result, 100.0);
              case TOTAL_VOLUME, POWER ->
                      formatAndProcess(testStationId, heatingCalculationType, result, 1000.0);
              case VOLUME_FLOW -> formatAndProcess(testStationId, heatingCalculationType, result, 60.0);
              case GENERATED_ENERGY_HEATING, RETURN_TEMPERATURE, SUPPLY_TEMPERATURE ->
                      processAndPushCurrentResults(testStationId, heatingCalculationType.name(), result);
              default -> log.warn("Unhandled HeatingCalculationType: {}", heatingCalculationType);
            }
          } catch (ModbusDeviceException | WaitingRoomException | TimeoutException | IllegalStateException |
                   ModbusTransportException ex) {
            log.error("Exception [{} - {}]: {}", heatingCalculationType.name(), testStationId, ex.getMessage(), ex);
            Thread.currentThread().interrupt();
            throw new HeatingProcessingException("Critical failure in energy processing", ex);
          } catch (Exception ex) {
            log.error("Unexpected exception [{} - {}]: {}", heatingCalculationType.name(), testStationId, ex.getMessage(), ex);
            Thread.currentThread().interrupt();
            throw new InterruptedException("Thread interrupted due to exception: " + ex.getMessage());
          }
        }
      }
    }
  }


  public boolean hasHeatingSubDevice(int testStationId) {
    TestStation testStation = testStationService.getTestStationById(testStationId);
    if (testStation == null) return true;
    return testStation.getModbusDevices().stream().flatMap(md -> md.getSubDevices().stream()).noneMatch(sd -> sd.getType() == SubDeviceType.HEATING);
  }


  public void processAndPushCurrentResults(int testStationId, String key, Object value) {
    log.debug("Processing value: Key = {}, Value = {}, testStationId = {}", key, value, testStationId);
    try {
      Object lastValue = lastHeatingResults.get(key);
      log.debug("🔎 Previous value for key '{}': {}", key, lastValue);

      boolean valueChanged = (lastValue == null || !lastValue.equals(value));

      log.debug("🗂 Before update - currentChpResults[{}]: {}", key, currentHeatingResults.get(key));

      currentHeatingResults.put(key, value);
      log.debug("📌 After update - currentChpResults[{}]: {}", key, currentHeatingResults.get(key));

      if (valueChanged) {
        lastHeatingResults.put(key, value);
        Map<String, Object> update = new LinkedHashMap<>();
        update.put(key, value);
        update.put("testStationId", testStationId);

        if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
          webSocketHandlerCustom.enqueueUpdate(update);
        }
      } else {
        log.debug("Heating value did not change, skipping push for key: {}, testStationId: {}", key, testStationId);
      }
    } catch (Exception e) {
      log.error("Unexpected error in heating processAndPush for key: {}, testStationId: {} - {}", key, testStationId, e.getMessage(), e);
    }
  }


  public void calculateAndPushHeatingDifference(int testStationId) throws InterruptedException {
    log.info("Calculating heating difference for testStationId: {}", testStationId);
    log.info("Waiting for gasService to populate first results before calculating heating difference for testStationId: {}", testStationId);
    if (!gasService.getIsInitialResultsPopulated().get()) {
      return;
    }

    try {
      List<SubDevice> heatingSubDevices = testStationService.getSubDevicesByType(testStationId, SubDeviceType.HEATING);

      if (heatingSubDevices == null || heatingSubDevices.isEmpty()) {
        log.warn("No Heating SubDevices found for testStationId: {}", testStationId);
        return;
      }

      for (SubDevice subDevice : heatingSubDevices) {
        List<HeatingCalculationType> types = subDevice.getHeatingCalculationTypes();
        if (types == null || types.isEmpty()) {
          log.debug("No HeatingCalculationTypes for SubDevice [slaveId={}, startAddress={}, type={}]", subDevice.getSlaveId(), subDevice.getStartAddress(), subDevice.getType());
          continue;
        }


        for (HeatingCalculationType type : types) {
          if (type != GENERATED_ENERGY_HEATING) continue;

          String key = type.name();
          Object current = currentHeatingResults.get(key);
          Object first = initialHeatingResults.get(key);


          if (current == null || first == null || current.equals(first)) continue;


          if (currentHeatingResults.get(key) != null && initialHeatingResults.get(key) != null) {

            try {
              double currentVal = parseDouble(current);
              double firstVal = parseDouble(first);
              double diff = currentVal - firstVal;


              log.info("Current [{}]: {}, First [{}]: {}, Difference: {}", key, currentVal, key, firstVal, diff);
              if (!Double.isNaN(diff) && !Double.isInfinite(diff)) {
                heatingDifference.set(diff);
              }


              Map<String, Object> update = new LinkedHashMap<>();
              update.put(key, String.format(Locale.US, "%.2f", heatingDifference.get()));
              update.put("testStationId", testStationId);
              update.put("difference", key);

              if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
                webSocketHandlerCustom.enqueueUpdate(update);
              }

            } catch (NumberFormatException nfe) {
              log.error("Invalid number format for key {}: {}", key, nfe.getMessage(), nfe);
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Failed to calculate heating difference for testStationId {}: {}", testStationId, e.getMessage(), e);
      Thread.currentThread().interrupt();
      throw new InterruptedException("Thread interrupted due to exception: " + e.getMessage());
    }
  }


  private double parseDouble(Object value) throws NumberFormatException {
    return (value instanceof Number) ? ((Number) value).doubleValue() : Double.parseDouble(value.toString());
  }


  public synchronized Map<String, Object> getStartHeatingResults(int testStationId) {
    log.debug("Saving current heating results as start results for testStationId: {}", testStationId);
    initialHeatingResults.putAll(currentHeatingResults);
    return new HashMap<>(initialHeatingResults);
  }

  public Map<String, Object> getLastHeatingResults(int testStationId) {
    log.debug("Fetching last heating results for testStationId: {}", testStationId);
    return new HashMap<>(lastHeatingResults);
  }

  public synchronized void clearHeatingResults() {
    initialHeatingResults.clear();
    currentHeatingResults.clear();
    lastHeatingResults.clear();
    heatingDifference.set(Double.NaN);
  }
}


