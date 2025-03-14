package com.example.backend.config;

import com.example.backend.service.EnergyService;
import com.example.backend.service.HeatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ModbusPollingService {

  private final EnergyService energyService;
  private final HeatingService heatingService;
  public boolean isRunning = false;
  private Thread pollingThread;  // Variable to store the current polling thread

  @Autowired
  public ModbusPollingService(EnergyService energyService, HeatingService heatingService) {
    this.energyService = energyService;
    this.heatingService = heatingService;
  }

  public void startPolling(int deviceId) {
    if (pollingThread != null && pollingThread.isAlive()) {
      // If polling thread is already running, stop it
      isRunning = false;  // Stop the current polling thread
      try {
        pollingThread.join();  // Wait for the polling thread to stop gracefully
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    clearPreviousResults();
    // Start a new polling thread for the new device
    isRunning = true;
    pollingThread = new Thread(() -> pollModbusData(deviceId));
    pollingThread.start();  // Start the new polling thread
  }

  public void stopPolling() {
    if (pollingThread != null && pollingThread.isAlive()) {
      isRunning = false;
      try {
        pollingThread.join();  // Wait for the thread to finish
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  // Polling loop for Modbus data
  private void pollModbusData(int deviceId) {
    try {
      while (isRunning) {
        startParallelProcessing(deviceId);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      System.out.println("Polling thread stopped.");
    }
  }

  public void startParallelProcessing(int deviceId) {
    // Create two separate threads for energy and heating data processing
    Thread energyThread = new Thread(() -> energyService.processEnergyData(deviceId));
    Thread heatingThread = new Thread(() -> heatingService.processHeatingData(deviceId));

    // Start both threads simultaneously
    energyThread.start();
    heatingThread.start();

    try {
      // Wait for both threads to finish
      energyThread.join();
      heatingThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }


  }

  private void clearPreviousResults() {
    // Clear energy data
    energyService.getFirstEnergyResults().clear();
    energyService.getCurrentEnergyResults().clear();
    energyService.getLastEnergyResults().clear();

  }
}