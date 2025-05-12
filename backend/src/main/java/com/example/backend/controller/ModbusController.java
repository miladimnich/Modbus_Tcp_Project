package com.example.backend.controller;

import com.example.backend.config.DeviceConfig;
import com.example.backend.config.ModbusPollingService;
import com.example.backend.models.TestStation;
import com.example.backend.service.BhkwService;
import com.example.backend.service.DeviceService;
import com.example.backend.service.EnergyService;
import com.example.backend.service.GasService;
import com.example.backend.service.HeatingService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class ModbusController {
  private final DeviceService deviceService;
  private final DeviceConfig deviceConfig;
  private final ModbusPollingService modbusPollingService;
  private final EnergyService energyService;
  private final HeatingService heatingService;
  private final GasService gasService;
  private final BhkwService bhkwService;


  @Autowired
  public ModbusController(DeviceService deviceService, DeviceConfig deviceConfig, ModbusPollingService modbusPollingService, EnergyService energyService, HeatingService heatingService, GasService gasService, BhkwService bhkwService) {
    this.deviceService = deviceService;
    this.deviceConfig = deviceConfig;
    this.modbusPollingService = modbusPollingService;
    this.energyService = energyService;
    this.heatingService = heatingService;
    this.gasService = gasService;
    this.bhkwService = bhkwService;
   }

  @GetMapping("/devices")
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
    return ResponseEntity.ok("Started pushing energy data for device " + deviceId);
  }

  @PostMapping("/{deviceId}/startMeasure")
  public ResponseEntity<Map<String, Object>> startTask(@PathVariable int deviceId) {
    modbusPollingService.startMeasureTask(deviceId);


    Map<String, Long> firstEnergyResults = energyService.startResults(deviceId);
    Map<String, Long> firsHeatingtResults = heatingService.startResults(deviceId);
    Map<String, Object> firstGasResults = gasService.startResults(deviceId);
    Map<String, Long> firstBhkwResults = bhkwService.startResults(deviceId);


    Map<String, Object> combinedResults = new LinkedHashMap<>();
    combinedResults.putAll(firstEnergyResults);
    combinedResults.putAll(firsHeatingtResults);
    combinedResults.putAll(firstGasResults);
    combinedResults.putAll(firstBhkwResults);


    return ResponseEntity.ok(combinedResults);
  }

  @PostMapping("/{deviceId}/stopMeasure")
  public ResponseEntity<Map<String, Object>> stopTask(@PathVariable int deviceId) {

    Map<String, Long> lastEnergyResults = energyService.lastResults(deviceId);
    Map<String, Long> lastHeatingResults = heatingService.lastResults(deviceId);
    Map<String, Object> lastGasResults = gasService.lastResults(deviceId);
    Map<String, Long> lastBhkwResults = bhkwService.lastResults(deviceId);


    // Combine both results into a single map
    Map<String, Object> combinedResults = new LinkedHashMap<>();
    combinedResults.putAll(lastEnergyResults);
    combinedResults.putAll(lastHeatingResults);
    combinedResults.putAll(lastGasResults);
    combinedResults.putAll(lastBhkwResults);


    return ResponseEntity.ok(combinedResults);
  }
}

