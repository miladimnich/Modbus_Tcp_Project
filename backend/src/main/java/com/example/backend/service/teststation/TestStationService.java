package com.example.backend.service.teststation;

import com.example.backend.config.TestStationConfig;
import com.example.backend.enums.SubDeviceType;
import com.example.backend.exception.TestStationNotFoundException;
import com.example.backend.models.ModbusDevice;
import com.example.backend.models.SubDevice;
import com.example.backend.models.TestStation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TestStationService {

  private final TestStationConfig testStationConfig;

  public TestStationService(TestStationConfig testStationConfig) {
    this.testStationConfig = testStationConfig;
  }

  public List<TestStation> getAllTestStations() {
    log.info("Fetching all test stations.");
    return testStationConfig.getTestStations();
  }


  public TestStation getTestStationById(int testStationId) {
    log.debug("Searching for TestStation with ID: {}", testStationId);

    return testStationConfig.getTestStations().stream()
            .filter(station -> station.getId() == testStationId)
            .findFirst()
            .orElseThrow(() -> {
              log.warn("TestStation with ID {} not found", testStationId);
              return new TestStationNotFoundException("TestStation with id " + testStationId + " not found");
            });
  }


  public List<SubDevice> getSubDevicesByType(int testStationId, SubDeviceType subDeviceType) {
    log.debug("Fetching SubDevices of type {} for TestStation ID {}", subDeviceType, testStationId);

    TestStation testStation = getTestStationById(testStationId);  // already logs & throws if not found

    List<SubDevice> subDevices = new ArrayList<>();
    for (ModbusDevice modbusDevice : testStation.getModbusDevices()) {
      for (SubDevice subDevice : modbusDevice.getSubDevices()) {
        if (subDevice.getType() == subDeviceType) {
          subDevices.add(subDevice);
        }
      }
    }

    if (subDevices.isEmpty()) {
      log.info("No SubDevices of type {} found for TestStation ID {}", subDeviceType, testStationId);
    } else {
      log.info("Found {} SubDevices of type {} for TestStation ID {}", subDevices.size(), subDeviceType, testStationId);
    }

    return subDevices;
  }
}
