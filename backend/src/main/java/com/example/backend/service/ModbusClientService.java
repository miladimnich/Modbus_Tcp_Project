package com.example.backend.service;

import com.example.backend.models.Device;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.ip.IpParameters;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ModbusClientService {

  private final DeviceService deviceService;
  private final Map<Integer, ModbusMaster> modbusMasters = new ConcurrentHashMap<>();  // Map deviceId to ModbusMaster
  private final ModbusFactory modbusFactory = new ModbusFactory();

  @Autowired
  public ModbusClientService(DeviceService deviceService) {
    this.deviceService = deviceService;
  }


  @PostConstruct
  public void init() {
    setupModbusMasters();
  }

  private void setupModbusMasters() {
    for (Device device : deviceService.getAllDevices()) {
      createModbusMaster(device);
    }
  }

  private void createModbusMaster(Device device) {
    try {
      IpParameters params = new IpParameters();
      params.setHost(device.getIpAddress());
      params.setPort(device.getPort());
      params.setEncapsulated(false);

      ModbusMaster modbusMaster = modbusFactory.createTcpMaster(params, true);
      // Set timeout and retries
      modbusMaster.setTimeout(5000); // Set timeout to 5000 milliseconds (5 seconds)
      modbusMaster.setRetries(3);    // Set retries to 3
     // modbusMaster.init();
      // Store ModbusMaster in map using deviceId
      modbusMasters.put(device.getId(), modbusMaster);

    } catch (Exception e) {
      e.printStackTrace();

    }
  }

  public ModbusMaster getModbusMasterbyId(int deviceId) {
    return modbusMasters.get(deviceId);
  }

  public synchronized void updateDevice() {
    shutdown(); // close existing connection
    modbusMasters.clear(); // clear the map
    setupModbusMasters();// reinitialize with updated devices

  }

  @PreDestroy
  private void shutdown() {
    for(ModbusMaster master: modbusMasters.values()){
      try {
        master.destroy();
      }catch (Exception e){
        e.printStackTrace();
      }
    }
  }

}
