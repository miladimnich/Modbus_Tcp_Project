package com.example.backend.service;

import com.example.backend.exception.ModbusIOException;
import com.example.backend.models.ModbusDevice;
import com.example.backend.models.TestStation;
import com.example.backend.service.DeviceService;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.ip.IpParameters;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ModbusClientService {

  private final DeviceService deviceService;
  private final Map<Integer, List<ModbusMaster>> modbusMasters = new ConcurrentHashMap<>();

  private final ModbusFactory modbusFactory = new ModbusFactory();
  private final Map<Integer, List<IpParameters>> modbusIps = new ConcurrentHashMap<>();

  @Autowired
  public ModbusClientService(DeviceService deviceService) {
    this.deviceService = deviceService;

  }


  @PostConstruct
  public void init() {
    setupModbusMasters();
  }


  private void setupModbusMasters() {
    for (TestStation testStation : deviceService.getAllTestStations()) {
      createModbusMaster(testStation);
    }
  }


  private void createModbusMaster(TestStation testStation) {
    try {
      List<ModbusMaster> modbusMastersList = new ArrayList<>();
      List<IpParameters> ipParametersList = new ArrayList<>();
      for (ModbusDevice device : testStation.getModbusdevices()) {
        String ipAddress = device.getIpAddress();
        int port = device.getPort();

        IpParameters params = new IpParameters();

        params.setHost(ipAddress);
        params.setPort(port);
        params.setEncapsulated(false);

        ModbusMaster modbusMaster = modbusFactory.createTcpMaster(params, true);

        modbusMaster.setTimeout(5000); // Set timeout to 5000 milliseconds (5 seconds)
        modbusMaster.setRetries(3);    // Set retries to 3


        try {
          //  modbusMaster.init();
          System.out.println("ModbusMaster initialized for device: " + testStation.getId() + ":" + ipAddress + ":" + port);
          modbusMastersList.add(modbusMaster);
          ipParametersList.add(params);
        } catch (ModbusIOException e) {
          System.err.println("Failed to initialize ModbusMaster for " + ipAddress + ":" + port + " - " + e.getMessage());
        }

      }

      modbusMasters.put(testStation.getId(), modbusMastersList);
      modbusIps.put(testStation.getId(), ipParametersList);
    } catch (Exception e) {
      e.printStackTrace();

    }
  }

  public List<ModbusMaster> getModbusMasterbyId(int testStationId) {
    return modbusMasters.get(testStationId);
  }

  public String getIpAddressFromModbusMaster(ModbusMaster modbusMaster, TestStation testStation) {
    // Get the list of IpParameters for the given TestStation ID
    List<IpParameters> ipParametersList = modbusIps.get(testStation.getId());

    // Check if the ModbusMaster corresponds to an IP in the list
    for (int i = 0; i < ipParametersList.size(); i++) {
      IpParameters ipParams = ipParametersList.get(i);
      if (modbusMaster.equals(modbusMasters.get(testStation.getId()).get(i))) {
        // Return the IP address stored in IpParameters
        return ipParams.getHost();
      }
    }

    return null; // Return null if no matching ModbusMaster is found
  }


  public synchronized void updateDevice() {
    shutdown(); // close existing connection
    modbusMasters.clear(); // clear the map
    setupModbusMasters();// reinitialize with updated devices
  }

  @PreDestroy
  private void shutdown() {
    for (List<ModbusMaster> masters : modbusMasters.values()) {
      for (ModbusMaster master : masters) {
        try {
          master.destroy();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
}




