package com.example.backend.controller;

import com.example.backend.config.DeviceConfig;
import com.example.backend.config.MeasurementSessionRegistry;
import com.example.backend.config.ModbusPollingService;
import com.example.backend.exception.PollingException;
import com.example.backend.models.TestStation;
import com.example.backend.models.ValueRange;
import com.example.backend.service.BhkwService;
import com.example.backend.service.DeviceService;
import com.example.backend.service.EnergyService;
import com.example.backend.service.GasService;
import com.example.backend.service.HeatingService;
import com.example.backend.service.MaschineTypeService;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/devices")
public class ModbusController {
  private final DeviceService deviceService;
  private final DeviceConfig deviceConfig;
  private final ModbusPollingService modbusPollingService;
  private final EnergyService energyService;
  private final HeatingService heatingService;
  private final GasService gasService;
  private final BhkwService bhkwService;
  private final MaschineTypeService maschineTypeService;
  private final MeasurementSessionRegistry measurementSessionRegistry;


  @Autowired
  public ModbusController(DeviceService deviceService, DeviceConfig deviceConfig, ModbusPollingService modbusPollingService, EnergyService energyService, HeatingService heatingService, GasService gasService, BhkwService bhkwService, MaschineTypeService maschineTypeService, MeasurementSessionRegistry measurementSessionRegistry) {
    this.deviceService = deviceService;
    this.deviceConfig = deviceConfig;
    this.modbusPollingService = modbusPollingService;
    this.energyService = energyService;
    this.heatingService = heatingService;
    this.gasService = gasService;
    this.bhkwService = bhkwService;
    this.maschineTypeService = maschineTypeService;
    this.measurementSessionRegistry = measurementSessionRegistry;
  }

  @GetMapping
  public ResponseEntity<List<TestStation>> getDevices() {
    if (deviceConfig.getTestStations().isEmpty()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(null);
    } else {
      List<TestStation> testStations = deviceService.getAllTestStations();
      return ResponseEntity.ok(testStations);
    }
  }

  @PostMapping("/{deviceId}")
  public ResponseEntity<String> startPollingForDevice(@PathVariable int deviceId) {
    try {
      modbusPollingService.startPolling(deviceId);
      return ResponseEntity.ok("Started pushing energy data for device " + deviceId);
    } catch (PollingException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to start polling for device " + deviceId + ": " + e.getMessage());
    }
  }

  @GetMapping("/{deviceId}/borders")
  public ResponseEntity<Map<String, ValueRange>> getDeviceBorders(@PathVariable int deviceId) {


    Map<String, ValueRange> borders = maschineTypeService.getDefaultValues(deviceId);
    if (borders.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(borders);
  }

  @PostMapping("/{deviceId}/startMeasure")
  public ResponseEntity<Map<String, Object>> startTask(@PathVariable int deviceId, @RequestBody Map<String, Object> body) {


    int serienNummer = Integer.parseInt((String) body.get("serienNummer"));

    modbusPollingService.startMeasureTask(deviceId);


    while (!gasService.isFirstResultsPopulated()) {
      try {
        // Sleep briefly to avoid constant checking and overload
        Thread.sleep(500);  // Wait for 500ms before checking again
      } catch (InterruptedException e) {
        // Handle thread interruption gracefully
        Thread.currentThread().interrupt();  // Restore interrupt status

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Collections.singletonMap("error", "Error while waiting for gas results"));
      }
    }


    Map<String, Object> firstEnergyResults = energyService.startResults(deviceId);
    Map<String, Object> firsHeatingtResults = heatingService.startResults(deviceId);
    Map<String, Object> firstGasResults = gasService.getFirstResults();
    Map<String, Object> firstBhkwResults = bhkwService.startResults(deviceId);

    Map<String, Object> combinedResults = new LinkedHashMap<>();
    combinedResults.putAll(firstEnergyResults);
    combinedResults.putAll(firsHeatingtResults);
    combinedResults.putAll(firstGasResults);
    combinedResults.putAll(firstBhkwResults);

    long startTime = gasService.getNow();

    measurementSessionRegistry.registerSerienNummer(deviceId, serienNummer);

    Map<String, Object> response = new HashMap<>();
    response.put("initialData", combinedResults);
    response.put("startTime", startTime);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/{deviceId}/stopMeasure")
  public ResponseEntity<Map<String, Object>> stopTask(@PathVariable int deviceId) {


    Map<String, Object> lastEnergyResults = energyService.lastResults(deviceId);
    Map<String, Object> lastHeatingResults = heatingService.lastResults(deviceId);
    Map<String, Object> lastGasResults = gasService.getLastResults();
    Map<String, Object> lastBhkwResults = bhkwService.lastResults(deviceId);


    // Combine both results into a single map
    Map<String, Object> combinedResults = new LinkedHashMap<>();
    combinedResults.putAll(lastEnergyResults);
    combinedResults.putAll(lastHeatingResults);
    combinedResults.putAll(lastGasResults);
    combinedResults.putAll(lastBhkwResults);


    Map<String, Object> response = new HashMap<>();
    response.put("lastData", combinedResults);
    response.put("endTime", modbusPollingService.getEndTime());

    return ResponseEntity.ok(response);
  }
}

