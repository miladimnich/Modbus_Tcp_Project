package com.example.backend.service;

import com.example.backend.config.DeviceConfig;
import com.example.backend.exception.DeviceNorFoundException;
import com.example.backend.models.Device;
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

  public List<Device> getAllDevices() {
    return deviceConfig.getDevices();
  }


  public Device getDeviceById(int deviceId) {
    for (Device device : deviceConfig.getDevices()) {
      if (device.getId() == deviceId) {
        return device;
      }
    }
    throw new DeviceNorFoundException("Device with id " + deviceId + " not found");
  }

}
