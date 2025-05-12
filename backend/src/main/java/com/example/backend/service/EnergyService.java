package com.example.backend.service;


import com.example.backend.config.ModbusLock;
import com.example.backend.config.WebSocketHandlerCustom;
import com.example.backend.enums.EnergyCalculationType;
import com.example.backend.enums.SubDeviceType;
import com.example.backend.exception.ModbusDeviceException;
import com.example.backend.models.ModbusDevice;
import com.example.backend.models.SubDevice;
import com.example.backend.models.TestStation;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.messaging.TimeoutException;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EnergyService {

  @Getter
  private final Map<String, Long> firstEnergyResults = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Long> currentEnergyResults = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Long> lastEnergyResults = new ConcurrentHashMap<>();


  private final ModbusBitwiseService modbusBitwiseService;
  private final DeviceService deviceService;
  private final WebSocketHandlerCustom webSocketHandlerCustom;


  @Autowired
  public EnergyService(ModbusBitwiseService modbusBitwiseService, DeviceService deviceService,
      WebSocketHandlerCustom webSocketHandlerCustom) {
    this.modbusBitwiseService = modbusBitwiseService;
    this.deviceService = deviceService;
    this.webSocketHandlerCustom = webSocketHandlerCustom;


  }

  public void processEnergyData(int deviceId) throws InterruptedException {
    TestStation testStation = deviceService.getTestStationById(deviceId);
    for (ModbusDevice modbusDevice : testStation.getModbusdevices()) {
      List<SubDevice> energySubDevices = modbusDevice.getSubDevices();
      for (SubDevice subDevice : energySubDevices) {
        if (subDevice.getType().equals(SubDeviceType.ENERGY)) {
          List<EnergyCalculationType> energyCalculationTypes = subDevice.getEnergyCalculationTypes();

          long totalStartTime = System.currentTimeMillis();
          for (EnergyCalculationType energyCalculationType : energyCalculationTypes) {
            int startAddress = energyCalculationType.getStartAddress(subDevice);

            try {
              long result;
              switch (energyCalculationType) {
                case ERZEUGTE_ENERGIE, GENUTZTE_ENERGIE, WIRKLEISTUNG, FREQUENZ, SPANNUNG_L3_VOLTS,
                     SPANNUNG_L2_VOLTS, SPANNUNG_L1_VOLTS, SCHEINLEISTUNG_RESERVED,
                     BLINDLEISTUNG_REACTIVPOWER, STROM:
                  result = modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress,
                      modbusDevice, subDevice.getType());
                  System.out.println(
                      "Calculated Value: " + energyCalculationType.name() + " = " + result);
                  processAndPush(deviceId, energyCalculationType.name(), result);
                  break;
                default:
                  System.out.println("Unhandled EnergyCalculationType: " + energyCalculationType);
                  break;
              }
            } catch (ModbusDeviceException ex) {
              String errorMessage =
                  "ModbusDeviceException error during Modbus calculation for device " + deviceId
                      + " (" + energyCalculationType.name() + "): " + ex.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;
            } catch (WaitingRoomException e) {
              System.err.println(
                  "WaitingRoomException while processing gas data for device " + deviceId + ": "
                      + e.getMessage());
              Thread.currentThread().interrupt();
              return;
            } catch (TimeoutException e) {
              System.err.println(
                  "TimeoutException while processing gas data for device " + deviceId + ": "
                      + e.getMessage());
              Thread.currentThread().interrupt();
              return;
            } catch (IllegalStateException ex) {
              String errorMessage = "Error during Modbus calculation for device " + deviceId + " ("
                  + energyCalculationType.name() + "): " + ex.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;
            } catch (ModbusTransportException e) {
              String errorMessage =
                  "ModbusTransportException  " + deviceId + " (" + energyCalculationType.name()
                      + "): " + e.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;
            } catch (Exception ex) {
              String errorMessage =
                  "Unexpected error during Modbus calculation for device " + deviceId + " ("
                      + energyCalculationType.name() + "): " + ex.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;
            }
          }
          long totalEndTime = System.currentTimeMillis();  // End timing after loop
          System.out.println("Total time to implement all switch tasks for Energy: " + (totalEndTime
              - totalStartTime) + " ms");
        }
      }
    }
  }


  public void processAndPush(int deviceId, String key, long value) {
    System.out.println(
        "Processing value: Key = " + key + ", Value = " + value + ", DeviceId = " + deviceId);
    boolean valueChanged = !lastEnergyResults.containsKey(key);

    currentEnergyResults.put(key, value);
    lastEnergyResults.putIfAbsent(key, value);

    if (!currentEnergyResults.get(key).equals(lastEnergyResults.get(key))) {
      lastEnergyResults.put(key, value);
      valueChanged = true;
    }

    if (valueChanged) {
      Map<String, Object> update = new LinkedHashMap<>();
      update.put(key, currentEnergyResults.get(key));
      update.put("deviceId", deviceId);

      if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
        try {
          System.out.println("Enqueuing data: " + update);
          webSocketHandlerCustom.enqueueUpdate(update);
        } catch (Exception e) {
          System.err.println("Error while enqueuing WebSocket update: " + e.getMessage());
        }
      } else {
        System.out.println("WebSocket session is no longer available. Skipping WebSocket push.");
      }

    } else {
      System.out.println("Value did not change, skipping push: " + key);
    }
  }

  public Map<String, Long> startResults(int deviceId) {
    firstEnergyResults.putAll(currentEnergyResults);
    return firstEnergyResults;
  }

  public Map<String, Long> lastResults(int deviceId) {
    return lastEnergyResults;
  }


}