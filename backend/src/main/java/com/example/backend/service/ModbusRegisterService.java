package com.example.backend.service;

import com.example.backend.config.WebSocketHandlerCustom;
import com.example.backend.models.SubDevice;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class ModbusRegisterService {
  private final DeviceService deviceService;
  private final ModbusClientService modbusClientService;
  @Autowired
  @Lazy  // Lazy initialization of WebSocket to avoid circular dependency
  private WebSocketHandlerCustom webSocketHandlerCustom;

  public ModbusRegisterService(DeviceService deviceService, ModbusClientService modbusClientService) {
    this.deviceService = deviceService;
    this.modbusClientService = modbusClientService;

  }

  public List<Map<String, Object>> getRegistersForDevice(int deviceId, int startAddress) {
    List<Map<String, Object>> allRegisters = new ArrayList<>();
    for (SubDevice subDevice : deviceService.getDeviceById(deviceId).getSubDevices()) {
      List<Map<String, Object>> subDeviceRegisters = readHoldingRegisters(deviceId, subDevice.getSlaveId(), startAddress, subDevice.getRegistersQuantity());
      allRegisters.addAll(subDeviceRegisters);
    }
    return allRegisters;
  }

  private List<Map<String, Object>> readHoldingRegisters(int deviceId, int slaveId, int startAddress, int quantity) {
    List<Map<String, Object>> allProcessedRegisters = new ArrayList<>();
    ModbusMaster modbusMaster = modbusClientService.getModbusMasterbyId(deviceId);
    if (modbusMaster != null) {
      try {
        ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, startAddress, quantity);
        ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) modbusMaster.send(request);
        short[] shortData = response.getShortData();
        for (int i = 0; i < shortData.length; i++) {
          Map<String, Object> register = new HashMap<>();
          register.put("address", startAddress + i);
          register.put("value", shortData[i] & 0xFFFF); // Convert to unsigned
          allProcessedRegisters.add(register);
        }
      } catch (ModbusTransportException e) {
        e.printStackTrace();
      }

    }
    return allProcessedRegisters;
  }

}
