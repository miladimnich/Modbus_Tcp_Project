package com.example.backend.service.energy;

import com.example.backend.config.socket.WebSocketHandlerCustom;
import com.example.backend.enums.EnergyCalculationType;
import com.example.backend.enums.SubDeviceType;
import com.example.backend.exception.EnergyProcessingException;
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

import static com.example.backend.enums.EnergyCalculationType.COS_PHI;
import static com.example.backend.enums.EnergyCalculationType.GENERATED_ENERGY;


@Slf4j
@Service
@Getter
@Setter
public class EnergyService {

  private final Map<String, Object> initialEnergyResults = new ConcurrentHashMap<>();
  private final Map<String, Object> currentEnergyResults = new ConcurrentHashMap<>();
  private final Map<String, Object> lastEnergyResults = new ConcurrentHashMap<>();
  private final AtomicReference<Double> energyDifference = new AtomicReference<>(Double.NaN);
  private final AtomicReference<Long> activePower = new AtomicReference<>(null);
  private final AtomicReference<Long> apparentPowerReserved = new AtomicReference<>(null);


  private final ModbusBitwiseService modbusBitwiseService;
  private final TestStationService testStationService;
  private final WebSocketHandlerCustom webSocketHandlerCustom;
  private final GasService gasService;


  @Autowired
  public EnergyService(ModbusBitwiseService modbusBitwiseService, TestStationService testStationService,
                       WebSocketHandlerCustom webSocketHandlerCustom, GasService gasService) {
    this.modbusBitwiseService = modbusBitwiseService;
    this.testStationService = testStationService;
    this.webSocketHandlerCustom = webSocketHandlerCustom;
    this.gasService = gasService;
  }

  private void formatAndProcess(int testStationId, EnergyCalculationType type, long rawValue, double divisor) {
    double value = (double) rawValue / divisor;
    String formattedResult = String.format(Locale.US, "%.2f", value);
    processAndPushCurrentResults(testStationId, type.name(), formattedResult);
  }


  public void processEnergyData(int testStationId) throws InterruptedException {
    log.info("Starting energy data processing for TestStation ID: {}", testStationId);
    TestStation testStation = testStationService.getTestStationById(testStationId);
    if (testStation == null) {
      log.warn("No TestStation found for ID: {}", testStationId);
      return;
    }
    for (ModbusDevice modbusDevice : testStation.getModbusDevices()) {
      for (SubDevice subDevice : modbusDevice.getSubDevices()) {
        if (!subDevice.getType().equals(SubDeviceType.ENERGY)) {
          log.debug("Skipping SubDevice with slaveId={}, startAddress={}, type={}",
                  subDevice.getSlaveId(), subDevice.getStartAddress(), subDevice.getType());
          continue;
        }
        List<EnergyCalculationType> energyCalculationTypes = subDevice.getEnergyCalculationTypes();
        if (energyCalculationTypes == null) continue;
        for (EnergyCalculationType energyCalculationType : energyCalculationTypes) {
          int startAddress = energyCalculationType.getStartAddress(subDevice);
          try {
            log.debug("Calculating {} for SubDevice ID: {} at address {}", energyCalculationType.name(), subDevice.getType(), startAddress);
            long result = modbusBitwiseService.bitwiseShiftCalculation(
                    testStationId, startAddress, modbusDevice, subDevice);
            log.info("result for start address {} is {}", startAddress, result);
            switch (energyCalculationType) {
              case GENERATED_ENERGY, CONSUMED_ENERGY, CURRENT ->
                      formatAndProcess(testStationId, energyCalculationType, result, 1000.0);
              case REACTIVE_POWER_BLIND_POWER ->
                      formatAndProcess(testStationId, energyCalculationType, result, 10000.0);
              case ACTIVE_POWER -> {
                activePower.set(result);
                formatAndProcess(testStationId, energyCalculationType, result, 10000.0);
              }
              case APPARENT_POWER_RESERVED -> {
                apparentPowerReserved.set(result);
                formatAndProcess(testStationId, energyCalculationType, result, 10000.0);
              }
              case FREQUENCY, VOLTAGE_L1_VOLTS, VOLTAGE_L2_VOLTS, VOLTAGE_L3_VOLTS ->
                      formatAndProcess(testStationId, energyCalculationType, result, 10.0);
              default -> log.warn("Unhandled EnergyCalculationType: {}", energyCalculationType);
            }

          } catch (InterruptedException ie) {
            log.warn("Thread interrupted during energy data processing for testStationId {}", testStationId, ie);
            Thread.currentThread().interrupt(); // preserve interrupt status
            throw ie; // propagate InterruptedException
          } catch (ModbusDeviceException | WaitingRoomException | TimeoutException |
                   IllegalStateException | ModbusTransportException ex) {
            log.error("Exception [{} - {}]: {}", energyCalculationType.name(), testStationId, ex.getMessage(), ex);
            throw new EnergyProcessingException("Critical failure in energy processing", ex);
          } catch (Exception ex) {
            log.error("Unexpected exception [{} - {}]: {}", energyCalculationType.name(), testStationId, ex.getMessage(), ex);
            Thread.currentThread().interrupt();
            throw new InterruptedException("Thread interrupted due to exception: " + ex.getMessage());
          }
        }
        // Now calculate cos_phi after switch
        long ap = activePower.get();
        long apr = apparentPowerReserved.get();

        if (apr != 0 && ap != 0) {
          double resultCosPhi = (double) ap / apr;
          String formattedCosPhi = String.format(Locale.US, "%.2f", resultCosPhi);
          log.info("Calculated cos_phi: {}", formattedCosPhi);
          processAndPushCurrentResults(testStationId, COS_PHI.name(), formattedCosPhi);
        } else {
          log.warn("Apparent Power or Active Power is zero, cannot calculate cos_phi");
        }
      }
    }
  }


  private void processAndPushCurrentResults(int testStationId, String key, Object value) {
    log.debug("Processing value: Key = {}, Value = {}, testStationId = {}", key, value, testStationId);
    try {
      Object lastValue = lastEnergyResults.get(key); // at the beginning its null after value for the specific key
      log.debug("🔎 Previous value for key '{}': {}", key, lastValue);
      boolean valueChanged = (lastValue == null || !lastValue.equals(value));
      log.debug("🗂 Before update - currentChpResults[{}]: {}", key, currentEnergyResults.get(key));

      currentEnergyResults.put(key, value);
      log.debug("📌 After update - currentChpResults[{}]: {}", key, currentEnergyResults.get(key));

      if (valueChanged) { // true means lastEnergyResults empty or the value differs
        lastEnergyResults.put(key, value);
        Map<String, Object> update = new LinkedHashMap<>();
        update.put(key, value);
        update.put("testStationId", testStationId);
        log.debug("✅ Value changed, enqueuing update: {}", update);
        if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
          webSocketHandlerCustom.enqueueUpdate(update);
        }
      } else {
        log.debug("Value did not change, skipping push for key: {}, testStationId: {}", key, testStationId);
      }
    } catch (Exception e) {
      log.error("Unexpected error in energy  processAndPush for key: {}, testStationId: {} - {}", key, testStationId, e.getMessage(), e);
    }
  }

  public void calculateAndPushEnergyDifference(int testStationId) throws InterruptedException {
    log.info("Calculating energy difference for testStationId: {}", testStationId);
    log.info("Waiting for gasService to populate first results before calculating energy difference for testStationId: {}", testStationId);

    if (!gasService.getIsInitialResultsPopulated().get()) {
      return;
    }
    try {
      List<SubDevice> energySubDevices = testStationService.getSubDevicesByType(testStationId, SubDeviceType.ENERGY);

      if (energySubDevices == null || energySubDevices.isEmpty()) {
        log.warn("No ENERGY SubDevices found for testStationId: {}", testStationId);
        return;
      }

      for (SubDevice subDevice : energySubDevices) {
        List<EnergyCalculationType> types = subDevice.getEnergyCalculationTypes();

        if (types == null || types.isEmpty()) {
          log.debug("No EnergyCalculationTypes for SubDevice [slaveId={}, startAddress={}, type={}]",
                  subDevice.getSlaveId(), subDevice.getStartAddress(), subDevice.getType());
          continue;
        }

        for (EnergyCalculationType type : types) {
          if (type != GENERATED_ENERGY) continue;

          String key = type.name();
          Object current = currentEnergyResults.get(key);
          Object first = initialEnergyResults.get(key);

          if (current == null || first == null || current.equals(first)) continue;

          try {
            double currentVal = parseDouble(current);
            double firstVal = parseDouble(first);
            double diff = currentVal - firstVal;

            log.info("Current [{}]: {}, First [{}]: {}, Difference: {}", key, currentVal, key, firstVal, diff);

            if (!Double.isNaN(diff) && !Double.isInfinite(diff)) {
              energyDifference.set(diff);
            }

            Map<String, Object> update = new LinkedHashMap<>();
            update.put(key, String.format(Locale.US, "%.2f", energyDifference.get()));
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
    } catch (Exception e) {
      log.error("Failed to calculate energy difference for testStationId {}: {}", testStationId, e.getMessage(), e);
      Thread.currentThread().interrupt();
      throw new InterruptedException("Thread interrupted due to exception: " + e.getMessage());
    }
  }



  private double parseDouble(Object value) throws NumberFormatException {
    return (value instanceof Number) ? ((Number) value).doubleValue() : Double.parseDouble(value.toString());
  }


  public synchronized Map<String, Object> getStartEnergyResults(int testStationId) {
    log.debug("Saving current energy results as start results for testStationId: {}", testStationId);
    initialEnergyResults.putAll(currentEnergyResults);
    return new HashMap<>(initialEnergyResults);  // return a copy
  }

  public Map<String, Object> getLastEnergyResults(int testStationId) {
    log.debug("Fetching last energy results for testStationId: {}", testStationId);
    return new HashMap<>(lastEnergyResults);  // return a safe copy, no sync needed
  }


  public synchronized void clearEnergyResults() {
    initialEnergyResults.clear();
    currentEnergyResults.clear();
    lastEnergyResults.clear();
    energyDifference.set(Double.NaN);
    activePower.set(null);
    apparentPowerReserved.set(null);
  }
}

