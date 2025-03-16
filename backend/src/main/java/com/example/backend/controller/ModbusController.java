package com.example.backend.controller;

import com.example.backend.config.ModbusPollingService;
import com.example.backend.models.Device;
import com.example.backend.models.TestStation;
import com.example.backend.service.DeviceService;
import com.example.backend.service.EnergyService;
import com.example.backend.service.HeatingService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
    private final ModbusPollingService modbusPollingService;
    private final EnergyService energyService;
    private final HeatingService heatingService;


    @Autowired
    public ModbusController(DeviceService deviceService, ModbusPollingService modbusPollingService,
        EnergyService energyService, HeatingService heatingService) {
      this.deviceService = deviceService;
      this.modbusPollingService = modbusPollingService;
      this.energyService = energyService;

      this.heatingService = heatingService;

    }

    @GetMapping("/devices")
    public ResponseEntity<List<TestStation>> getDevices() {
    //  List<Device> devices = deviceService.getAllDevices();
      List<TestStation> stations = deviceService.getAllTestStations();
      return ResponseEntity.ok(stations);
    }

    @PostMapping("devices/{deviceId}")
    public ResponseEntity<String> getCurrentValue(@PathVariable int deviceId) {
      modbusPollingService.startPolling(deviceId);
      return ResponseEntity.ok("Started pushing energy data for device " + deviceId);
    }

    @PostMapping("devices/{deviceId}/startMeasure")
    public ResponseEntity<Map<String, Long>> startTask(@PathVariable int deviceId) {
      if (modbusPollingService.isRunning) {
        Map<String, Long> firstEnergyResults = energyService.startResults(deviceId);
        Map<String, Long> firsHeatingtResults = heatingService.startResults(deviceId);
        // Combine both results into a single map
        Map<String, Long> combinedResults = new LinkedHashMap<>();
        combinedResults.putAll(firstEnergyResults);  // Add energy results
        combinedResults.putAll(firsHeatingtResults); // Add heating results
        return ResponseEntity.ok(combinedResults);
      } else {
        Map<String, Long> errorResponse = new LinkedHashMap<>();
        errorResponse.put("error", -1L); // -1 indicates an error or that polling is not running
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

      }
    }

    @PostMapping("devices/{deviceId}/stopMeasure")
    public ResponseEntity<Map<String, Long>> stopTask(@PathVariable int deviceId) {
      modbusPollingService.stopPolling();
      Map<String, Long> lastEnergyResults = energyService.lastResults(deviceId);
      Map<String, Long> lastHeatingResults = heatingService.lastResults(deviceId);
      // Combine both results into a single map
      Map<String, Long> combinedResults = new LinkedHashMap<>();
      combinedResults.putAll(lastEnergyResults);  // Add energy results
      combinedResults.putAll(lastHeatingResults); // Add heating results
      return ResponseEntity.ok(combinedResults);
    }


  }
