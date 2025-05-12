package com.example.backend.service;

import static com.example.backend.enums.GasCalculationType.GAS_ZAHLER;

import com.example.backend.config.WebSocketHandlerCustom;
import com.example.backend.enums.GasCalculationType;
import com.example.backend.enums.GasData;
import com.example.backend.enums.SubDeviceType;
import com.example.backend.events.GasCalculationCompleteEvent;
import com.example.backend.events.StopPollingEvent;
import com.example.backend.exception.ModbusDeviceException;
import com.example.backend.models.ModbusDevice;
import com.example.backend.models.SubDevice;
import com.example.backend.models.TestStation;
import com.example.backend.service.DeviceService;
import com.example.backend.service.ModbusBitwiseService;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.messaging.TimeoutException;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomException;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
  private final Map<String, Object> previousResults = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Object> accumulatedDifference = new ConcurrentHashMap<>();


  private final AtomicReference<Double> totalDifference = new AtomicReference<>(Double.NaN);


  private final AtomicReference<Double> environmentPressure = new AtomicReference<>(Double.NaN);
  private final AtomicReference<Double> gasPressure = new AtomicReference<>(Double.NaN);
  private final AtomicReference<Double> gasTempereture = new AtomicReference<>(Double.NaN);


  private final AtomicReference<Double> gasLeistungResult = new AtomicReference<>(Double.NaN);


  public Double getGasLeistungResult() {
    return gasLeistungResult.get();
  }

  public void clearGasLeistungResult() {
    gasLeistungResult.set(Double.NaN);
  }

  @Getter
  @Setter
  private volatile boolean isFirstResultsPopulated = false;
  @Getter
  private final AtomicBoolean shouldStop = new AtomicBoolean(false);


  private final ScheduledExecutorService executorService;
  private final List<ScheduledFuture<?>> scheduledTasks = new ArrayList<>();


  @Getter
  private long autoStopDurationMinutes = 1;

  private final ModbusBitwiseService modbusBitwiseService;
  private final DeviceService deviceService;
  private final WebSocketHandlerCustom webSocketHandlerCustom;

  @Getter
  private volatile Long now;


  @Autowired
  public GasService(ScheduledExecutorService executorService, ModbusBitwiseService modbusBitwiseService,
      DeviceService deviceService, WebSocketHandlerCustom webSocketHandlerCustom) {
    this.executorService = executorService;

    this.modbusBitwiseService = modbusBitwiseService;

    this.deviceService = deviceService;
    this.webSocketHandlerCustom = webSocketHandlerCustom;

  }


  @Autowired
  private ApplicationEventPublisher publisher;


  public Double getEnvironmentPressure() {
    return environmentPressure.get();
  }

  public void clearEnvironmentPressure() {
    environmentPressure.set(Double.NaN);
  }


  public Double getGasPressure() {
    return gasPressure.get();
  }

  public void clearGasPressure() {
    gasPressure.set(Double.NaN);
  }

  public Double getGasTempereture() {
    return gasTempereture.get();
  }

  public void clearGasTempereture() {
    gasTempereture.set(Double.NaN);
  }


  public Double getTotalDifference() {
    return totalDifference.get();
  }

  public void clearTotalDifference() {
    totalDifference.set(Double.NaN);
  }


  public Map<String, Object> lastResults(int deviceId) {
    return lastResults;
  }



  public void processAndPushCurrentResults(int deviceId, String key, Object value) {
    System.out.println("Processing value: Key = " + key + ", Value = " + value + ", DeviceId = " + deviceId);

    boolean valueChanged = !lastResults.containsKey(key); //true

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
              System.out.println("Processing " + gasCalculationType + " at address " + startAddress);

              switch (gasCalculationType) {
                case GAS_TEMPERATUR, GAS_ZAHLER, GAS_DRUCK, UMGEBUNGS_TEMPERATURE,
                     UMGEBUNGS_DRUCK:

                  long startTime = System.currentTimeMillis();
                  result = modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress, modbusDevice, subDevice);
                  long duration = System.currentTimeMillis() - startTime;
                  System.out.println("Time taken for " + gasCalculationType + ": " + duration + "ms " + result);
                  // Handle gas pressure calculations with specific formatting
                  if (gasCalculationType == GasCalculationType.GAS_DRUCK) {
                    if (startAddress == 33) {
                      gasPressure.set(result * 0.07398380 + 2.869148);
                    } else if (startAddress == 35) {
                      gasPressure.set(result * 0.0740486 + 0.0327669);
                    } else if (startAddress == 37) {
                      gasPressure.set(result * 0.074172 + 4.156304);
                    }
                  } else if (gasCalculationType == GasCalculationType.GAS_TEMPERATUR || gasCalculationType == GasCalculationType.UMGEBUNGS_TEMPERATURE) {
                    gasTempereture.set(result / 100.0);
                    String formattedResult = String.format(Locale.US, "%.2f", gasTempereture.get());
                    System.out.println("Calculated Value: " + gasCalculationType.name() + " = " + result);

                    processAndPushCurrentResults(deviceId, gasCalculationType.name(), formattedResult);
                  } else if (gasCalculationType == GasCalculationType.UMGEBUNGS_DRUCK) {
                    environmentPressure.set(result * 0.074064361 - 1.176470588);
                    String formattedResult = String.format(Locale.US, "%.2f", environmentPressure.get());
                    System.out.println("Calculated Value: " + gasCalculationType.name() + " = " + result);

                    processAndPushCurrentResults(deviceId, gasCalculationType.name(), formattedResult);
                  } else {
                    System.out.println("Calculated Value: " + gasCalculationType.name() + " = " + result);
                    processAndPushCurrentResults(deviceId, gasCalculationType.name(), result);
                    System.out.println("skipped for " + gasCalculationType + " with result: " + result);
                  }

                  if (gasCalculationType == GasCalculationType.GAS_DRUCK) {
                    double value = gasPressure.get() - environmentPressure.get();
                    String formattedResult = String.format(Locale.US, "%.2f", value);
                    processAndPushCurrentResults(deviceId, gasCalculationType.name(), formattedResult);
                  }
                  break;
                default:
                  System.out.println("Unhandled GasCalculationType: " + gasCalculationType);
                  return;
              }
            } catch (ModbusDeviceException ex) {
              String errorMessage = "ModbusDeviceException error during Modbus calculation for device " + deviceId + " (" + gasCalculationType.name() + "): " + ex.getMessage();
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
              String errorMessage = "Error during Modbus calculation for device " + deviceId + " (" + gasCalculationType.name() + "): " + ex.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;
            } catch (ModbusTransportException e) {
              String errorMessage = "ModbusTransportException  " + deviceId + " (" + gasCalculationType.name() + "): " + e.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;

            } catch (Exception ex) {
              String errorMessage = "Unexpected error during Modbus calculation for device " + deviceId + " (" + gasCalculationType.name() + "): " + ex.getMessage();
              System.err.println(errorMessage);
              Thread.currentThread().interrupt();
              return;
            }
          }
          long totalEndTime = System.currentTimeMillis();  // End timing after loop
          System.out.println("Total time to implement all switch tasks for Gas: " + (totalEndTime - totalStartTime) + " ms");
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
        if (gasCalculationType.equals(GAS_ZAHLER)) {

          String key = gasCalculationType.name();

          if (currentResults.get(key) != null) {

            double currentDouble = Double.parseDouble(currentResults.get(key).toString());

            int currentR = (int) currentDouble;


            if (!previousResults.containsKey(key)) {
              System.out.println("Previous value not found for key: " + key + ". Initializing.");
              previousResults.put(key, currentR); // Initialize it
              return; // Do not calculate difference yet
            }

            int previous = Integer.parseInt(previousResults.get(key).toString());

            double count = (previous != currentR) ? 0.1 : 0.0; // if true 0.1 else 0.0
            if (count == 0) {
              return;
            }
            if (!isFirstResultsPopulated) {
              if (firstResults.isEmpty()) {

                now = System.currentTimeMillis();


                firstResults.putAll(currentResults);

                previousResults.put(key, currentR);

                isFirstResultsPopulated = true;


                //       publisher.publishEvent(new FirstResultsAvailableEvent(this, deviceId));

                ScheduledFuture<?> stopTask = executorService.schedule(() -> {
                  // Check for interruption before executing the task
                  if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Stop task interrupted before execution.");
                    return;  // Exit early if interrupted
                  }

                  try {
                    // Your task logic here
                    shouldStop.set(true);
                    System.out.println("Stop task executed after " + autoStopDurationMinutes + " minutes.");
                  } catch (Exception e) {
                    // Handle any exceptions that occur during the task execution
                    System.err.println("Error executing stop task: " + e.getMessage());
                  }
                }, autoStopDurationMinutes, TimeUnit.MINUTES);

                scheduledTasks.add(stopTask);

                return;
              }
            }


            if (Double.isNaN(totalDifference.get())) {
              totalDifference.set(0.0);
            }

            totalDifference.updateAndGet(val -> val + count);

            accumulatedDifference.put(key, totalDifference);


            Map<String, Object> update = new LinkedHashMap<>();
            update.put(key, String.format(Locale.US, "%.2f", totalDifference.get()));
            update.put("deviceId", deviceId);
            update.put("difference", key);

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

            berechnungGasLeistung(deviceId, environmentPressure.get(), gasPressure.get(), gasTempereture.get(), totalDifference.get());

            if(shouldStop.get()){
              publisher.publishEvent(new StopPollingEvent(this, deviceId));
              return;
            }
            previousResults.put(key, currentR);

          }
        }
      }
    }
  }



  private void berechnungGasLeistung(int deviceId, Double environmentPressure, Double gasPressure,
      Double gasTempereture, Double gasMeter) {
    if (gasMeter.isNaN()) {
      return;
    }

    double result = (10.01 * 0.9017) * ((environmentPressure + gasPressure) / 1013.15)
        * (273.0 / (273.0 + gasTempereture)) * gasMeter;

    System.out.println("GasLeistung result " + result);

    gasLeistungResult.set(result);

    String formattedResult = String.format(Locale.US, "%.2f", gasLeistungResult.get());
    Map<String, Object> update = new LinkedHashMap<>();
    update.put(GasData.GAS_LEISTUNG.name(), formattedResult);
    update.put("deviceId", deviceId);
    webSocketHandlerCustom.enqueueUpdate(update);

    publisher.publishEvent(new GasCalculationCompleteEvent(this, deviceId));

  }






  public void cancelScheduledTasks() {
    for (ScheduledFuture<?> task : scheduledTasks) {
      if (task != null && !task.isCancelled()) {
        task.cancel(false);
      }
    }
    scheduledTasks.clear();
  }

  @PreDestroy
  public void shutdown() {
    try {
      if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
        executorService.shutdownNow();  // Force shutdown if the tasks don't finish within the timeout
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();  // If the current thread is interrupted, force shutdown
      Thread.currentThread().interrupt();
    }
  }

}

























