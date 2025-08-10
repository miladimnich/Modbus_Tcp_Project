package com.example.backend.controller;

 import com.example.backend.exception.PollingException;
import com.example.backend.models.TestStation;
import com.example.backend.models.ValueRange;
import com.example.backend.service.machine.MachineTypeService;
import com.example.backend.service.polling.ModbusPollingService;
import com.example.backend.service.polling.PollingControlService;
import com.example.backend.service.teststation.TestStationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@RestController
@RequestMapping("/api/testStations")
public class ModbusController {
    private final TestStationService testStationService;
    private final ModbusPollingService modbusPollingService;
    private final MachineTypeService machineTypeService;
    private final PollingControlService pollingControlService;
    private final ScheduledExecutorService executor;

    public ModbusController(TestStationService testStationService, ModbusPollingService modbusPollingService, MachineTypeService machineTypeService, PollingControlService pollingControlService, ScheduledExecutorService executor ) {
        this.testStationService = testStationService;
        this.modbusPollingService = modbusPollingService;
        this.machineTypeService = machineTypeService;
        this.pollingControlService = pollingControlService;
        this.executor = executor;
     }

    @GetMapping
    public ResponseEntity<List<TestStation>> getTestStations() {
        try {
            List<TestStation> testStations = testStationService.getAllTestStations();
            if (testStations.isEmpty()) {
                log.warn("No test stations configured or available");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Collections.emptyList());  // safer than null body
            } else {
                log.info("Returning {} test stations", testStations.size());
                return ResponseEntity.ok(testStations);
            }
        } catch (Exception e) {
            log.error("Failed to fetch test stations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @PostMapping("/{testStationId}")
    public ResponseEntity<String> startPollingForTestStation(@PathVariable int testStationId) {
        try {
            modbusPollingService.startPolling(testStationId);
            log.info("Started polling for test station {}", testStationId);
            return ResponseEntity.ok("Started pushing data for test station " + testStationId);
        } catch (PollingException e) {
            log.error("Failed to start polling for test station {}", testStationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to start polling for test station " + testStationId + ": " + e.getMessage());
        }
    }


    @PostMapping("/{testStationId}/startMeasure")
    public ResponseEntity<Void> startTask(@PathVariable int testStationId) {
        CompletableFuture.runAsync(() -> {
            try {
             pollingControlService.startMeasurement(testStationId);
            } catch (Exception e) {
                log.error("Failed to start measurement for testStationId {}: {}", testStationId, e.getMessage(), e);
            }
        }, executor);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/{testStationId}/stopMeasure")
    public ResponseEntity<Void> stopTask(@PathVariable int testStationId) {
        pollingControlService.stopMeasurement(testStationId);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/borders")
    public ResponseEntity<Map<String, ValueRange>> getTestStationBorders() {
        Map<String, ValueRange> borders = machineTypeService.getDefaultValues();
        if (borders.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(borders);
    }
}

