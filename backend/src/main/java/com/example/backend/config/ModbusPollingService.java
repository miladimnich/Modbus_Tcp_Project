package com.example.backend.config;

import com.example.backend.service.BhkwService;
import com.example.backend.service.EnergyService;
import com.example.backend.service.GasService;
import com.example.backend.service.HeatingService;
import com.example.backend.service.StartPollingEvent;
import com.example.backend.service.StopPollingEvent;
 import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
 import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.Synchronized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ModbusPollingService {

  private final EnergyService energyService;
  private final HeatingService heatingService;
  private final GasService gasService;
  private final BhkwService bhkwService;
  public volatile boolean isRunning = false;
  int currentDeviceId = -1;
  private final WebSocketHandlerCustom webSocketHandlerCustom;
  private final List<ScheduledFuture<?>> scheduledTasks = new ArrayList<>();
  private final ScheduledExecutorService executorService;
  private boolean isMeasureStarted = false;



  @Autowired
  public ModbusPollingService(EnergyService energyService, HeatingService heatingService,
      GasService gasService, BhkwService bhkwService, WebSocketHandlerCustom webSocketHandlerCustom, ScheduledExecutorService executorService) {
    this.energyService = energyService;
    this.heatingService = heatingService;
    this.gasService = gasService;
    this.bhkwService = bhkwService;
    this.webSocketHandlerCustom = webSocketHandlerCustom;
    this.executorService = executorService;
  }

  @EventListener
  public void handleStartPollingEvent(StartPollingEvent event) {
    startPolling(event.getDeviceId());

  }

  @EventListener
  public void handleStopPollingEvent(StopPollingEvent event) {
    stopPolling(event.getDeviceId());
  }


  @Synchronized
  public void startPolling(int deviceId) {
    if (deviceId != currentDeviceId && isRunning) {
      stopPolling(currentDeviceId);
      try {
        Thread.sleep(100); // Short delay before restarting
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.out.println("Polling was interrupted during sleep. Aborting the operation.");
        return;
      }
    }
    if (isRunning || isMeasureStarted) {
      System.out.println("Polling is stopping. Cannot start new polling.");
      return; // Prevent starting new polling if it's already running
    }
    clearPreviousResults();
    currentDeviceId = deviceId;
    isRunning = true;

    webSocketHandlerCustom.startWebSocketUpdateTask();  // Start sending updates

    scheduledTasks.add(executorService.scheduleWithFixedDelay(() -> {
      try {
        synchronized (this) { // Prevent overlapping executions
          try {
            if (Thread.currentThread().isInterrupted()) {
              System.out.println("Thread was interrupted, stopping task...");
              cancelScheduledTasks();
              return; // Exit early
            }
            energyService.processEnergyData(deviceId);
          } catch (InterruptedException e) {
            System.out.println("Thread interrupted during data processing");
            stopPolling(deviceId);
          }
          System.out.println("Completed processEnergyData for device " + deviceId);

          try {
            if (Thread.currentThread().isInterrupted()) {
              System.out.println("Thread was interrupted, stopping task...");
              cancelScheduledTasks();
              return; // Exit early
            }
            heatingService.processHeatingData(deviceId);
          } catch (InterruptedException e) {
            System.out.println("Thread interrupted during data processing");
            stopPolling(deviceId);
          }

          System.out.println("Completed processHeatingData for device " + deviceId);

        }
      } catch (Exception e) {
        System.err.println("Error in scheduled task: " + e.getMessage());
      }
    }, 0, 1, TimeUnit.SECONDS));


    scheduledTasks.add(executorService.scheduleWithFixedDelay(() -> {
      try {
        if (Thread.currentThread().isInterrupted()) {
          System.out.println("Thread was interrupted, stopping task...");
          cancelScheduledTasks();
          return; // Exit early
        }
        bhkwService.processDataBhkw(deviceId);
      } catch (InterruptedException e) {
        System.out.println("Thread interrupted during data processing");
        stopPolling(deviceId);
      }

      System.out.println("Completed processBhkwData for device " + deviceId);

    }, 0, 1, TimeUnit.SECONDS));

    scheduledTasks.add(executorService.scheduleWithFixedDelay(() -> {
      try {
        if (Thread.currentThread().isInterrupted()) {
          System.out.println("Thread was interrupted, stopping task...");
          cancelScheduledTasks();
          return; // Exit early
        }
        gasService.processGasData(deviceId);
      } catch (InterruptedException e) {
        System.out.println("Thread interrupted during data processing");
        stopPolling(deviceId);
      }
      System.out.println("Completed processGasData for device " + deviceId);
    }, 0, 1, TimeUnit.SECONDS));
  }


  public synchronized void startMeasureTask(int deviceId) {
    if (isMeasureStarted || isRunning) {
      System.out.println("Measure task is already started for device " + deviceId);
      return; // Prevent starting measure task again if already running
    }

    isMeasureStarted = true;  // Mark measure task as started

    scheduledTasks.add(executorService.scheduleWithFixedDelay(() -> {
      try {
        if (Thread.currentThread().isInterrupted()) {
          System.out.println("Thread was interrupted, stopping task...");
          cancelScheduledTasks();
          return; // Exit early
        }
        gasService.calculateAndPushMeterDifference(deviceId);  // Trigger gas calculation

      } catch (InterruptedException e) {
        System.out.println("Thread interrupted during data processing");
        stopPolling(deviceId);
      }

      System.out.println(Thread.currentThread().getName() + " for device " + deviceId);
    }, 0, 1, TimeUnit.SECONDS));
  }


  public synchronized void stopPolling(int deviceId) {
    isRunning = false;
    isMeasureStarted = false;
    cancelScheduledTasks();
    if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
      webSocketHandlerCustom.closeAllWebSocketSessions();
      System.out.println("WebSocket all connections closed.");
    }
    System.out.println("Polling stopped for device " + deviceId);
    currentDeviceId = -1;
  }


  private void clearPreviousResults() {
    energyService.getFirstEnergyResults().clear();
    energyService.getCurrentEnergyResults().clear();
    energyService.getLastEnergyResults().clear();
    heatingService.getFirstHeatingResults().clear();
    heatingService.getLastHeatingResults().clear();
    heatingService.getCurrentHeatingResults().clear();
    gasService.getFirstResults().clear();
    gasService.getCurrentResults().clear();
    gasService.getAccumulatedDifference().clear();
    gasService.getPreviousResults().clear();
    gasService.setTotalDifference(0);
    gasService.getLastResults().clear();
    bhkwService.getFirstResults().clear();
    bhkwService.getCurrentResults().clear();
    bhkwService.getLastResults().clear();
  }


  private void cancelScheduledTasks() {
    for (ScheduledFuture<?> task : scheduledTasks) {
      if (task != null && !task.isCancelled()) {
        task.cancel(false);// true interupts threads false not unless it finishes its current execution false: Task is cancelled, but not interrupted (it can finish what it's doing).
      }
    }
    scheduledTasks.clear();
  }


  @PreDestroy
  public void shutdown() {
    executorService.shutdown(); // Gracefully shuts down the executor service
    try {
      if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
        executorService.shutdownNow(); // Force shutdown if the tasks don't finish within the timeout
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow(); // If the current thread is interrupted, force shutdown
      Thread.currentThread().interrupt();
    }
  }
}

