package com.example.backend.service;


import com.example.backend.enums.SubDeviceType;
import com.example.backend.exception.ModbusDeviceException;
import com.example.backend.models.ModbusDevice;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.messaging.TimeoutException;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ModbusBitwiseService {

  private final ModbusRegisterService modbusRegisterService;

  @Autowired
  public ModbusBitwiseService(ModbusRegisterService modbusRegisterService) {
    this.modbusRegisterService = modbusRegisterService;
  }

  public long bitwiseShiftCalculation(int testStationId, int startAddress,
      ModbusDevice modbusDevice, SubDeviceType subDeviceType)
      throws WaitingRoomException, ModbusTransportException, TimeoutException, ModbusDeviceException {

    List<Map<String, Object>> registers = modbusRegisterService.getRegistersForDevice(testStationId,
        startAddress, modbusDevice, subDeviceType);

    long result;
    if (registers.isEmpty()) {
      System.err.println("No registers available for device " + modbusDevice.getIpAddress() +
          ". This could be due to a closed WebSocket session or Modbus communication failure.");

      throw new IllegalStateException(
          "No registers available for device " + modbusDevice.getIpAddress() +
              ". This could be due to a closed WebSocket session or Modbus communication failure.");
    } else if (registers.size() == 4) {
      int register1 = (int) registers.get(0).get("value");
      int register2 = (int) registers.get(1).get("value");
      int register3 = (int) registers.get(2).get("value");
      int register4 = (int) registers.get(3).get("value");
      result = ((long) register1 << 48) | ((long) register2 << 32) | ((long) register3 << 16)
          | (long) register4;

    } else if (registers.size() == 1) {
      int register1 = (int) registers.get(0).get("value");
      result = (long) register1;
    } else {
      throw new IllegalStateException(
          "Unexpected number of registers for bitwise shift calculation.");
    }

    return result;
  }
}