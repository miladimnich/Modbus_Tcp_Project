package com.example.backend.service;

import static com.example.backend.enums.GasCalculationType.GAS_METER;

import com.example.backend.config.WebSocketHandlerCustom;
import com.example.backend.enums.GasCalculationType;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GasService {

  @Getter
  private final Map<String, Object> firstResults = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Object> currentResults = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Object> lastResults = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Object> differenceResult = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Object> previousResults = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Object> accumulatedDifference = new ConcurrentHashMap<>();
  @Setter
  private double totalDifference = 0;

  private final ModbusBitwiseService modbusBitwiseService;
  private final DeviceService deviceService;
  private final WebSocketHandlerCustom webSocketHandlerCustom;


  @Autowired
  public GasService(ModbusBitwiseService modbusBitwiseService,
      DeviceService deviceService, WebSocketHandlerCustom webSocketHandlerCustom) {
    this.modbusBitwiseService = modbusBitwiseService;

    this.deviceService = deviceService;
    this.webSocketHandlerCustom = webSocketHandlerCustom;

  }


  public Map<String, Object> lastResults(int deviceId) {
    lastResults.putAll(currentResults);
    return lastResults;
  }

  public Map<String, Object> startResults(int deviceId) {
    firstResults.putAll(currentResults);
    return firstResults;
  }


  public void processAndPushCurrentResults(int deviceId, String key, Object value) {
    System.out.println(
        "Processing value: Key = " + key + ", Value = " + value + ", DeviceId = " + deviceId);

    boolean valueChanged = !lastResults.containsKey(key);

    currentResults.put(key, value);
    lastResults.putIfAbsent(key, value);

    if (!currentResults.get(key).equals(lastResults.get(key))) {
      lastResults.put(key, value);
      valueChanged = true;
    }

    if (valueChanged) {
      Map<String, Object> update = new LinkedHashMap<>();
      update.put(key, currentResults.get(key));
      update.put("deviceId", deviceId);

      // Re-check WebSocket session availability before pushing data
      if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
        System.out.println("Enqueuing data: " + update);
        webSocketHandlerCustom.enqueueUpdate(update);
      } else {
        System.out.println("WebSocket session is no longer available. Skipping WebSocket push.");
      }
    } else {
      System.out.println("Value did not change, skipping push: " + key);
    }
  }


  public void processGasData(int deviceId) throws InterruptedException {
    TestStation testStation = deviceService.getTestStationById(deviceId);

    // Loop through all Modbus devices
    for (ModbusDevice modbusDevice : testStation.getModbusdevices()) {
      List<SubDevice> heatingSubDevices = modbusDevice.getSubDevices();

      // Loop through all sub-devices of the current Modbus device
      for (SubDevice subDevice : heatingSubDevices) {
        if (subDevice.getType().equals(SubDeviceType.GAS)) {
          List<GasCalculationType> gasCalculationTypes = subDevice.getGasCalculationType();

          long totalStartTime = System.currentTimeMillis();

          for (GasCalculationType gasCalculationType : gasCalculationTypes) {
            int startAddress = gasCalculationType.getStartAddress(subDevice);

            try {
              long result;
              System.out.println(
                  "Processing " + gasCalculationType + " at address " + startAddress);

              switch (gasCalculationType) {
                case GAS_TEMPERATURE, GAS_METER, GAS_PRESSURE, ENVIRONMENT_TEMPERATURE,
                     ENVIRONMENT_PRESSURE:

                  long startTime = System.currentTimeMillis();
                  result = modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress,
                      modbusDevice, subDevice.getType());
                  long duration = System.currentTimeMillis() - startTime;
                  System.out.println(
                      "Time taken for " + gasCalculationType + ": " + duration + "ms " + result);
                  // Handle gas pressure calculations with specific formatting
                  if (gasCalculationType == GasCalculationType.GAS_PRESSURE) {
                    if (startAddress == 33) {
                      String formattedResult = String.format(Locale.US, "%.2f",
                          result * 0.07398380 + 2.869148);
                      System.out.println(
                          "Calculated Value: " + gasCalculationType.name() + " = " + result);

                      processAndPushCurrentResults(deviceId, gasCalculationType.name(),
                          formattedResult);
                    } else if (startAddress == 35) {
                      String formattedResult = String.format(Locale.US, "%.2f",
                          result * 0.0740486 + 0.0327669);
                      System.out.println(
                          "Calculated Value: " + gasCalculationType.name() + " = " + result);

                      processAndPushCurrentResults(deviceId, gasCalculationType.name(),
                          formattedResult);
                    } else if (startAddress == 37) {
                      String formattedResult = String.format(Locale.US, "%.2f",
                          result * 0.074172 + 4.156304);
                      System.out.println(
                          "Calculated Value: " + gasCalculationType.name() + " = " + result);

                      processAndPushCurrentResults(deviceId, gasCalculationType.name(),
                          formattedResult);
                    }
                  } else if (gasCalculationType == GasCalculationType.GAS_TEMPERATURE
                      || gasCalculationType == GasCalculationType.ENVIRONMENT_TEMPERATURE) {
                    String formattedResult = String.format(Locale.US, "%.2f", result / 100.0);
                    System.out.println(
                        "Calculated Value: " + gasCalculationType.name() + " = " + result);

                    processAndPushCurrentResults(deviceId, gasCalculationType.name(),
                        formattedResult);
                  } else if (gasCalculationType == GasCalculationType.ENVIRONMENT_PRESSURE) {
                    String formattedResult = String.format(Locale.US, "%.2f",
                        result * 0.074064361 - 1.176470588);
                    System.out.println(
                        "Calculated Value: " + gasCalculationType.name() + " = " + result);

                    processAndPushCurrentResults(deviceId, gasCalculationType.name(),
                        formattedResult);
                  } else {
                    System.out.println(
                        "Calculated Value: " + gasCalculationType.name() + " = " + result);
                    processAndPushCurrentResults(deviceId, gasCalculationType.name(), result);
                    System.out.println(
                        "skipped for " + gasCalculationType + " with result: " + result);

                  }
                  break;

                default:
                  System.out.println("Unhandled GasCalculationType: " + gasCalculationType);
                  return;
              }
            } catch (ModbusDeviceException ex) {
              String errorMessage =
                  "ModbusDeviceException error during Modbus calculation for device " + deviceId
                      + " (" + gasCalculationType.name() + "): " + ex.getMessage();
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
                  + gasCalculationType.name() + "): " + ex.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;
            } catch (ModbusTransportException e) {
              String errorMessage =
                  "ModbusTransportException  " + deviceId + " (" + gasCalculationType.name() + "): "
                      + e.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;

            } catch (Exception ex) {
              String errorMessage =
                  "Unexpected error during Modbus calculation for device " + deviceId + " ("
                      + gasCalculationType.name() + "): " + ex.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;
            }
          }
          long totalEndTime = System.currentTimeMillis();  // End timing after loop
          System.out.println(
              "Total time to implement all switch tasks for Gas: " + (totalEndTime - totalStartTime)
                  + " ms");
        }
      }
    }
  }


  public void calculateAndPushMeterDifference(int deviceId) throws InterruptedException {

    List<SubDevice> gasSubDevices = deviceService.getSubDevicesByType(deviceId,
        SubDeviceType.GAS);
    for (SubDevice subDevice : gasSubDevices) {
      List<GasCalculationType> gasCalculationTypes = subDevice.getGasCalculationType();
      for (GasCalculationType gasCalculationType : gasCalculationTypes) {
        if (gasCalculationType.equals(GAS_METER)) {

          String key = gasCalculationType.name();
          if (currentResults.get(key) != null) {

            int currentR = Integer.parseInt(
                currentResults.compute(key, (k, v) -> v == null ? "0" : v.toString()).toString());

            //int currentR = currentResults.containsKey(key) ? Integer.parseInt(currentResults.get(key).toString()) : 0;
            if (previousResults.isEmpty()) {
              previousResults.put(key, currentR);
              return;
            }
            int previous = previousResults.containsKey(key) ? Integer.parseInt(
                previousResults.get(key).toString()) : 0;

            double count = 0;
            if (previous != currentR) {
              count++;
              count *= 0.1;
            }

            if (count == 0) {
              return;
            }

            totalDifference += count;

            accumulatedDifference.put(key, totalDifference);
            previousResults.put(key, currentR);

            Map<String, Object> update = new LinkedHashMap<>();
            update.put(key, String.format(Locale.US, "%.2f", totalDifference));
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
              System.out.println(
                  "WebSocket session is no longer available. Skipping WebSocket push.");
            }
          }
        }
      }
    }
  }
}

