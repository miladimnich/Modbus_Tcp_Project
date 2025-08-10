package com.example.backend.service.teststation;

import com.example.backend.config.DeviceConfig;
import com.example.backend.enums.SubDeviceType;
import com.example.backend.exception.DeviceNotFoundException;
import com.example.backend.models.ModbusDevice;
import com.example.backend.models.SubDevice;
import com.example.backend.models.TestStation;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DeviceService {
  private final DeviceConfig deviceConfig;

  @Autowired
  public DeviceService(DeviceConfig deviceConfig) {
    this.deviceConfig = deviceConfig;
  }

  public List<TestStation> getAllTestStations() {
    log.info("Fetching all test stations.");
    return deviceConfig.getTestStations();
  }


  public TestStation getTestStationById(int testStationId) {
    log.debug("Searching for TestStation with ID: {}", testStationId);

    return deviceConfig.getTestStations().stream()
            .filter(station -> station.getId() == testStationId)
            .findFirst()
            .orElseThrow(() -> {
              log.warn("TestStation with ID {} not found", testStationId);
              return new DeviceNotFoundException("TestStation with id " + testStationId + " not found");
            });
  }


  public List<SubDevice> getSubDevicesByType(int testStationId, SubDeviceType subDeviceType) {
    log.debug("Fetching SubDevices of type {} for TestStation ID {}", subDeviceType, testStationId);

    TestStation testStation = getTestStationById(testStationId);  // already logs & throws if not found

    List<SubDevice> subDevices = new ArrayList<>();
    for (ModbusDevice modbusDevice : testStation.getModBusDevices()) {
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
