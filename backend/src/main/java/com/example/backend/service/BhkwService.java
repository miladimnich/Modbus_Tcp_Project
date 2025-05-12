package com.example.backend.service;

import static com.example.backend.enums.BhkwCalculationType.BETRIEBS_STUNDEN;
import static com.example.backend.enums.BhkwCalculationType.START_ANZAHL;

import com.example.backend.config.WebSocketHandlerCustom;
import com.example.backend.enums.BhkwCalculationType;
import com.example.backend.enums.SubDeviceType;
import com.example.backend.exception.ModbusDeviceException;
import com.example.backend.models.ModbusDevice;
import com.example.backend.models.SubDevice;
import com.example.backend.models.TestStation;
import com.example.backend.service.DeviceService;
import com.example.backend.service.ModbusBitwiseService;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.messaging.TimeoutException;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
public class BhkwService {
  @Getter
  private final Map<String, Object> firstResults = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Object> currentResults = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Object> lastResults = new ConcurrentHashMap<>();

  private final ModbusBitwiseService modbusBitwiseService;
  private final DeviceService deviceService;
  private final WebSocketHandlerCustom webSocketHandlerCustom;


  public BhkwService(DeviceService deviceService, WebSocketHandlerCustom webSocketHandlerCustom, ModbusBitwiseService modbusBitwiseService) {
    this.deviceService = deviceService;
    this.webSocketHandlerCustom = webSocketHandlerCustom;
    this.modbusBitwiseService = modbusBitwiseService;

  }


  public void processDataBhkw(int deviceId) throws InterruptedException {
    TestStation testStation = deviceService.getTestStationById(deviceId);
    for (ModbusDevice modbusDevice : testStation.getModbusdevices()) {


      List<SubDevice> bhkwSubDevices = modbusDevice.getSubDevices();
      for (SubDevice subDevice : bhkwSubDevices) {
        if (subDevice.getType().equals(SubDeviceType.BHKW)) {
          List<BhkwCalculationType> bhkwCalculationTypes = subDevice.getBhkwCalculationTypes();
          long totalStartTime = System.currentTimeMillis();

          System.out.println("BHKW Type List Size: " + bhkwCalculationTypes.size());
          for (BhkwCalculationType bhkwCalculationType : bhkwCalculationTypes) {
            // Check for interruption before each calculation
            if (Thread.currentThread().isInterrupted()) {
              System.out.println("Thread interrupted while processing for device " + deviceId);
              return;  // Return early if the thread is interrupted
            }
            System.out.println("type " + bhkwCalculationType + " start Address " + bhkwCalculationType.getStartAddress(subDevice));
            int startAddress = bhkwCalculationType.getStartAddress(subDevice);

            try {
              long result;
              switch (bhkwCalculationType) {

                case ABGAS_TEMPERATUR, HEIZUNGS_WASSER_VORLAUF, HEIZUNGS_WASSER_RUCKLAUF,
                     MOTOR_KUHLMITTEL_RUCKLAUF, MOTOR_KUHLMITTEL_VORLAUF,
                     SCHALTSCHRANK, GEHAUSE, GENERATOR_WICKLUNG,
                     MOTOR_OIL, BETRIEBS_STUNDEN, START_ANZAHL:

                  result = modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress, modbusDevice, subDevice);

                  if (bhkwCalculationType.equals(BETRIEBS_STUNDEN) || bhkwCalculationType.equals(START_ANZAHL)) {
                    processAndPush(deviceId, bhkwCalculationType.name(), result);
                    break;
                  } else {
                    System.out.println("Calculated Value: " + bhkwCalculationType.name() + " = " + result);
                    String formattedResult = String.format(Locale.US, "%.2f", ((double) result / 10));

                    processAndPush(deviceId, bhkwCalculationType.name(), formattedResult);
                    break;
                  }
                default:
                  System.out.println("Unhandled EnergyCalculationType: " + bhkwCalculationType);
                  return;
              }
            } catch (ModbusDeviceException ex) {
              String errorMessage = "ModbusDeviceException error during Modbus calculation for device " + deviceId + " (" + bhkwCalculationType.name() + "): " + ex.getMessage();
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
              String errorMessage = "Error during Modbus calculation for device " + deviceId + " (" + bhkwCalculationType.name() + "): " + ex.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;
            } catch (ModbusTransportException e) {
              String errorMessage = "ModbusTransportException  " + deviceId + " (" + bhkwCalculationType.name() + "): " + e.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;
            } catch (Exception ex) {
              String errorMessage = "Unexpected error during Modbus calculation for device " + deviceId + " (" + bhkwCalculationType.name() + "): " + ex.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;
            }
          }
          long totalEndTime = System.currentTimeMillis();  // End timing after loop
          System.out.println("Total time to implement all switch tasks for Bhkw: " + (totalEndTime - totalStartTime) + " ms");
        }
      }
    }
  }

  public void processAndPush(int deviceId, String key, Object value) {
    System.out.println("Processing value: Key = " + key + ", Value = " + value + ", DeviceId = " + deviceId);

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


  public Map<String, Object> startResults(int deviceId) {
    firstResults.putAll(currentResults);
    return firstResults;
  }

  public Map<String, Object> lastResults(int deviceId) {
    return lastResults;
  }

}
