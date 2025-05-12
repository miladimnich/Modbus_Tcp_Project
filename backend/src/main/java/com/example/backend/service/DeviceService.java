package com.example.backend.service;

import com.example.backend.config.DeviceConfig;
import com.example.backend.enums.SubDeviceType;
import com.example.backend.exception.DeviceNotFoundException;
import com.example.backend.models.ModbusDevice;
import com.example.backend.models.SubDevice;
import com.example.backend.models.TestStation;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {
  private final DeviceConfig deviceConfig;

  @Autowired
  public DeviceService(DeviceConfig deviceConfig) {
    this.deviceConfig = deviceConfig;
  }

  public List<TestStation> getAllTestStations() {
    return deviceConfig.getTestStations();
  }

  public TestStation getTestStationById(int testStationId) {
    for (TestStation testStation : deviceConfig.getTestStations()) {
      if (testStation.getId() == testStationId) {
        return testStation;
      }
    }
    throw new DeviceNotFoundException("TestStation with id " + testStationId + " not found");
  }




  public List<SubDevice> getSubDevicesByType(int deviceId, SubDeviceType subDeviceType) {
    List<SubDevice> subDevices = new ArrayList<>();

    TestStation testStation = getTestStationById(deviceId);

    // Iterate over ModbusDevices and their SubDevices
    for (ModbusDevice modbusDevice : testStation.getModbusdevices()) {
      for (SubDevice subDevice : modbusDevice.getSubDevices()) {
        if (subDevice.getType() == subDeviceType) {

          subDevices.add(subDevice);
        }
      }
    }
    return subDevices;
  }

}

