package com.example.backend.config;

import com.example.backend.enums.SubDeviceType;
import com.example.backend.models.Device;
import com.example.backend.models.SubDevice;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class DeviceConfig {


  private final List<Device> devices = new ArrayList<>();

  @PostConstruct
  public void init() {
    devices.add(new Device(1, "TestStation1","192.168.120.11", 502,
        List.of(new SubDevice(1000,1,4, SubDeviceType.ENERGY)))
    );
    devices.add(new Device(2, "TestStation2", "192.168.120.11", 502,
        List.of(new SubDevice(2000, 1, 4, SubDeviceType.ENERGY),
            new SubDevice(12000, 1, 4, SubDeviceType.HEATING))
    ));
    devices.add(
        new Device(3, "TestStation3", "192.168.120.11", 502,
            List.of(new SubDevice(3000, 1, 4, SubDeviceType.ENERGY),
                new SubDevice(13000, 1, 4, SubDeviceType.HEATING))));
  }

}
