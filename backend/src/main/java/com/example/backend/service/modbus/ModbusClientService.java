package com.example.backend.service.modbus;

import com.example.backend.exception.ModbusIOException;
import com.example.backend.models.ModbusDevice;
import com.example.backend.models.TestStation;
import com.example.backend.service.teststation.TestStationService;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.ip.IpParameters;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ModbusClientService {

  private final TestStationService testStationService;
  private final Map<Integer, List<ModbusMaster>> modbusMasters = new ConcurrentHashMap<>();
  private final ModbusFactory modbusFactory = new ModbusFactory();
  private final Map<Integer, List<IpParameters>> modbusIps = new ConcurrentHashMap<>();

  @Autowired
  public ModbusClientService(TestStationService testStationService) {
    this.testStationService = testStationService;
  }


  @PostConstruct
  public void init() {
    setupModbusMasters();
  }


  private void setupModbusMasters() {
    for (TestStation testStation : testStationService.getAllTestStations()) {
      createModbusMaster(testStation);
    }
  }


  private void createModbusMaster(TestStation testStation) {
    try {
      List<ModbusMaster> modbusMastersList = new ArrayList<>();
      List<IpParameters> ipParametersList = new ArrayList<>();
      for (ModbusDevice device : testStation.getModbusDevices()) {
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
       //   modbusMaster.init();
          log.info("ModbusMaster initialized for device: {}:{}:{}", testStation.getId(), ipAddress, port);
          modbusMastersList.add(modbusMaster);
          ipParametersList.add(params);
        } catch (ModbusIOException e) {
          log.error("Failed to initialize ModbusMaster for {}:{} - {}", ipAddress, port, e.getMessage());
        }
      }
      modbusMasters.put(testStation.getId(), modbusMastersList);
      modbusIps.put(testStation.getId(), ipParametersList);
    } catch (Exception e) {
      log.error("Unexpected exception while creating ModbusMaster for TestStation {}: {}", testStation.getId(), e.getMessage(), e);
    }
  }

  public List<ModbusMaster> getModbusMasterById(int testStationId) {
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


  @PreDestroy
  private void shutdown() {
    for (List<ModbusMaster> masters : modbusMasters.values()) {
      for (ModbusMaster master : masters) {
        try {
          master.destroy();
        } catch (Exception e) {
          log.warn("Failed to destroy ModbusMaster: {}", e.getMessage(), e);
        }
      }
    }
    modbusMasters.clear();
  }
}



