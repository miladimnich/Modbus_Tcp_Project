package com.example.backend.service;

import static com.example.backend.enums.HeatingCalculationType.ERZEUGTE_ENERGIE_HEATING;

import com.example.backend.config.WebSocketHandlerCustom;
import com.example.backend.enums.HeatingCalculationType;
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
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
public class HeatingService {

  @Getter
  private final Map<String, Object> firstHeatingResults = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Object> currentHeatingResults = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Object> lastHeatingResults = new ConcurrentHashMap<>();


  private final AtomicReference<Double> heatingDifference = new AtomicReference<>(Double.NaN);


  private final WebSocketHandlerCustom webSocketHandlerCustom;


  private final ModbusBitwiseService modbusBitwiseService;
  private final DeviceService deviceService;

  public HeatingService(WebSocketHandlerCustom webSocketHandlerCustom,
      ModbusBitwiseService modbusBitwiseService, DeviceService deviceService) {
    this.webSocketHandlerCustom = webSocketHandlerCustom;
    this.modbusBitwiseService = modbusBitwiseService;
    this.deviceService = deviceService;
  }



  public Double getHeatingDifference() {
    return heatingDifference.get();
  }

  public void clearHeatingDifference() {
    heatingDifference.set(Double.NaN);
  }



  public void processHeatingData(int deviceId) throws InterruptedException{
    TestStation testStation = deviceService.getTestStationById(deviceId);

    if (!hasHeatingSubDevice(deviceId)) {
      System.out.println("No HEATING subdevice found for deviceId: " + deviceId + ". Skipping processHeatingData.");
      return;
    }


    for (ModbusDevice modbusDevice : testStation.getModbusdevices()) {
      List<SubDevice> heatingSubDevices = modbusDevice.getSubDevices();
      for (SubDevice subDevice : heatingSubDevices) {
        if (subDevice.getType().equals(SubDeviceType.HEATING)) {
          List<HeatingCalculationType> heatingCalculationTypes = subDevice.getHeatingCalculationTypes();

          long totalStartTime = System.currentTimeMillis();
          for (HeatingCalculationType heatingCalculationType : heatingCalculationTypes) {
            int startAddress = heatingCalculationType.getStartAddress(subDevice);

            try {
              long result;
              switch (heatingCalculationType) {
                case ERZEUGTE_ENERGIE_HEATING, VORLAUFTEMPERATUR, VOLUMENSTROM, TEMPERATURDIFFERENZ,
                     LEISTUNG, GESAMT_VOLUMEN, RuCKLAUFTEMPERATUR:

                  result = modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress, modbusDevice, subDevice);

                  System.out.println("Calculated Value: " + heatingCalculationType.name() + " = " + result);
                  if (heatingCalculationType == HeatingCalculationType.VORLAUFTEMPERATUR ||
                      heatingCalculationType == HeatingCalculationType.TEMPERATURDIFFERENZ ||
                      heatingCalculationType == HeatingCalculationType.RuCKLAUFTEMPERATUR
                  ) {
                    double value = (double) result / 100;
                    String formattedResult = String.format(Locale.US, "%.2f", value);
                    processAndPush(deviceId, heatingCalculationType.name(), formattedResult);

                  } else if (heatingCalculationType == HeatingCalculationType.GESAMT_VOLUMEN ||
                      heatingCalculationType == HeatingCalculationType.LEISTUNG) {
                    double value = (double) result / 1000;
                    String formattedResult = String.format(Locale.US, "%.2f", value);
                    processAndPush(deviceId, heatingCalculationType.name(), formattedResult);
                  } else if (heatingCalculationType == HeatingCalculationType.VOLUMENSTROM) {
                    double value = (double) result / 60;
                    String formattedResult = String.format(Locale.US, "%.2f", value);
                    processAndPush(deviceId, heatingCalculationType.name(), formattedResult);
                  } else {
                    processAndPush(deviceId, heatingCalculationType.name(), result);
                  }


                  break;
                default:
                  System.out.println("Unhandled heatingCalculationType: " + heatingCalculationType);
                  return;
              }
            } catch (ModbusDeviceException ex) {
              String errorMessage = "ModbusDeviceException error during Modbus calculation for device " + deviceId + " (" + heatingCalculationType.name() + "): " + ex.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
            } catch (WaitingRoomException e) {
              System.err.println("WaitingRoomException while processing gas data for device " + deviceId + ": " + e.getMessage());
              Thread.currentThread().interrupt();
            } catch (TimeoutException e) {
              System.err.println("TimeoutException while processing gas data for device " + deviceId + ": " + e.getMessage());
              Thread.currentThread().interrupt();
            } catch (IllegalStateException ex) {
              String errorMessage = "Error during Modbus calculation for device " + deviceId + " (" + heatingCalculationType.name() + "): " + ex.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
            } catch (ModbusTransportException e) {
              String errorMessage = "ModbusTransportException  " + deviceId + " (" + heatingCalculationType.name() + "): " + e.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();

            } catch (Exception ex) {
              String errorMessage = "Unexpected error during Modbus calculation for device " + deviceId + " (" + heatingCalculationType.name() + "): " + ex.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
            }

          }
          long totalEndTime = System.currentTimeMillis();  // End timing after loop
          System.out.println("Total time to implement all switch tasks for Heating: " + (totalEndTime - totalStartTime) + " ms");

        }
      }

    }
  }


  public boolean hasHeatingSubDevice(int deviceId) {
    TestStation testStation = deviceService.getTestStationById(deviceId);
    return testStation.getModbusdevices().stream()
        .flatMap(modbusDevice -> modbusDevice.getSubDevices().stream())
        .anyMatch(subDevice -> subDevice.getType() == SubDeviceType.HEATING);
  }


  public void processAndPush(int deviceId, String key, Object value) {
    System.out.println("Processing value: Key = " + key + ", Value = " + value + ", DeviceId = " + deviceId);

    boolean valueChanged = !lastHeatingResults.containsKey(key);

    currentHeatingResults.put(key, value);
    lastHeatingResults.putIfAbsent(key, value);

    if (!currentHeatingResults.get(key).equals(lastHeatingResults.get(key))) {
      lastHeatingResults.put(key, value);
      valueChanged = true;
    }

    if (valueChanged) {
      Map<String, Object> update = new LinkedHashMap<>();
      update.put(key, currentHeatingResults.get(key));
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


  public void calculateAndPushHeatingDifference(int deviceId) throws InterruptedException {
    List<SubDevice> heatingSubDevices = deviceService.getSubDevicesByType(deviceId,
        SubDeviceType.HEATING);
    for (SubDevice subDevice : heatingSubDevices) {
      List<HeatingCalculationType> heatingCalculationTypes = subDevice.getHeatingCalculationTypes();
      for (HeatingCalculationType heatingCalculationType : heatingCalculationTypes) {
        if (heatingCalculationType.equals(ERZEUGTE_ENERGIE_HEATING)) {

          String key = heatingCalculationType.name();
          if (currentHeatingResults.get(key) != null && firstHeatingResults.get(key) != null) {


            if (!currentHeatingResults.get(key).equals(firstHeatingResults.get(key))) {
              double currentResult = Double.parseDouble(currentHeatingResults.get(key).toString());
              double previousResult = Double.parseDouble(firstHeatingResults.get(key).toString());
              double difference = currentResult - previousResult;

              if (!Double.isNaN(difference) && !Double.isInfinite(difference)) {
                heatingDifference.set(difference);

              }


              Map<String, Object> update = new LinkedHashMap<>();
              update.put(key, String.format(Locale.US, "%.2f", heatingDifference.get()));
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
    firstHeatingResults.putAll(currentHeatingResults);
    return firstHeatingResults;
  }

  public Map<String, Object> lastResults(int deviceId) {
    return lastHeatingResults;
  }
}


