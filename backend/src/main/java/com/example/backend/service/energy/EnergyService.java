package com.example.backend.service;

import static com.example.backend.enums.EnergyCalculationType.ERZEUGTE_ENERGIE;

import com.example.backend.config.WebSocketHandlerCustom;
import com.example.backend.enums.EnergyCalculationType;
import com.example.backend.enums.SubDeviceType;
import com.example.backend.exception.ModbusDeviceException;
import com.example.backend.models.ModbusDevice;
import com.example.backend.models.SubDevice;
import com.example.backend.models.TestStation;
import com.example.backend.service.modbus.ModbusBitwiseService;
import com.example.backend.service.teststation.DeviceService;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.messaging.TimeoutException;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EnergyService {
  @Getter
  private final Map<String, Object> firstEnergyResults = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Object> currentEnergyResults = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Object> lastEnergyResults = new ConcurrentHashMap<>();
  private final AtomicReference<Double> energyDifference = new AtomicReference<>(Double.NaN);





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



  public Double getEnergyDifference() {
    return energyDifference.get();
  }

  public void clearEnergyDifference() {
    energyDifference.set(Double.NaN);
  }

  public void processEnergyData(int deviceId) throws InterruptedException {
    TestStation testStation = deviceService.getTestStationById(deviceId);
    for (ModbusDevice modbusDevice : testStation.getModBusDevices()) {
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
                          modbusDevice, subDevice);

                  if (energyCalculationType == ERZEUGTE_ENERGIE ||
                          energyCalculationType == EnergyCalculationType.GENUTZTE_ENERGIE ||
                          energyCalculationType == EnergyCalculationType.STROM) {
                    double value = (double) result / 1000;
                    String formattedResult = String.format(Locale.US, "%.2f", value);
                    processAndPush(deviceId, energyCalculationType.name(), formattedResult);
                  } else if (energyCalculationType == EnergyCalculationType.WIRKLEISTUNG ||
                          energyCalculationType == EnergyCalculationType.BLINDLEISTUNG_REACTIVPOWER ||
                          energyCalculationType == EnergyCalculationType.SCHEINLEISTUNG_RESERVED) {
                    double value = (double) result / 10000;
                    String formattedResult = String.format(Locale.US, "%.2f", value);
                    processAndPush(deviceId, energyCalculationType.name(), formattedResult);

                  } else {
                    double value = (double) result / 10;
                    String formattedResult = String.format(Locale.US, "%.2f", value);
                    processAndPush(deviceId, energyCalculationType.name(), formattedResult);
                    break;
                  }


                  break;

                default:
                  System.out.println("Unhandled EnergyCalculationType: " + energyCalculationType);
                  return;
              }
            } catch (ModbusDeviceException ex) {
              String errorMessage = "ModbusDeviceException error during Modbus calculation for device " + deviceId + " (" + energyCalculationType.name() + "): " + ex.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;
            } catch (WaitingRoomException e) {
              System.err.println("WaitingRoomException while processing gas data for device " + deviceId + ": " + e.getMessage());
              Thread.currentThread().interrupt();
              return;
            } catch (TimeoutException e) {
              System.err.println("TimeoutException while processing gas data for device " + deviceId + ": " + e.getMessage());
              Thread.currentThread().interrupt();
              return;
            } catch (IllegalStateException ex) {
              String errorMessage = "Error during Modbus calculation for device " + deviceId + " (" + energyCalculationType.name() + "): " + ex.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;
            } catch (ModbusTransportException e) {
              String errorMessage = "ModbusTransportException  " + deviceId + " (" + energyCalculationType.name() + "): " + e.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;
            } catch (Exception ex) {
              String errorMessage = "Unexpected error during Modbus calculation for device " + deviceId + " (" + energyCalculationType.name() + "): " + ex.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;
            }
          }
          long totalEndTime = System.currentTimeMillis();  // End timing after loop
          System.out.println("Total time to implement all switch tasks for Energy: " + (totalEndTime - totalStartTime) + " ms");
        }
      }
    }
  }


  public void processAndPush(int deviceId, String key, Object value) {
    System.out.println("Processing value: Key = " + key + ", Value = " + value + ", DeviceId = " + deviceId);
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

  public void calculateAndPushEnergyDifference(int deviceId) throws InterruptedException {
    List<SubDevice> energySubDevices = deviceService.getSubDevicesByType(deviceId,
            SubDeviceType.ENERGY);
    for (SubDevice subDevice : energySubDevices) {
      List<EnergyCalculationType> energyCalculationTypes = subDevice.getEnergyCalculationTypes();
      for (EnergyCalculationType energyCalculationType : energyCalculationTypes) {
        if (energyCalculationType.equals(ERZEUGTE_ENERGIE)) {

          String key = energyCalculationType.name();
          if (currentEnergyResults.get(key) != null && firstEnergyResults.get(key) != null) {


            if (!currentEnergyResults.get(key).equals(firstEnergyResults.get(key))) {
              double currentResult = Double.parseDouble(currentEnergyResults.get(key).toString());
              System.out.println("current result for ERZEUGTE_ENERGIE " + currentResult);
              double previousResult = Double.parseDouble(firstEnergyResults.get(key).toString());
              System.out.println("previous result for ERZEUGTE_ENERGIE " + previousResult);
              double difference = currentResult - previousResult;

              if (!Double.isNaN(difference) && !Double.isInfinite(difference)) {
                energyDifference.set(difference);

              }

              System.out.println("difference for ERZEUGTE_ENERGIE " + difference);

              Map<String, Object> update = new LinkedHashMap<>();
              update.put(key, String.format(Locale.US, "%.2f", energyDifference.get()));
              update.put("deviceId", deviceId);
              update.put("difference", key);
              // Re-check WebSocket session availability before pushing data
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

            }
          }
        }
      }
    }
  }

  public Map<String, Object> startResults(int deviceId) {
    firstEnergyResults.putAll(currentEnergyResults);

    return firstEnergyResults;
  }

  public Map<String, Object> lastResults(int deviceId) {
    return lastEnergyResults;
  }
}

