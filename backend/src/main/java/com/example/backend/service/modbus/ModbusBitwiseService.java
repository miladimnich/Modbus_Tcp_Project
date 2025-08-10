package com.example.backend.service.modbus;

import com.example.backend.exception.ModbusDeviceException;
import com.example.backend.models.ModbusDevice;
import com.example.backend.models.SubDevice;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.messaging.TimeoutException;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ModbusBitwiseService {
  private final ModbusRegisterService modbusRegisterService;

  public ModbusBitwiseService(ModbusRegisterService modbusRegisterService) {
    this.modbusRegisterService = modbusRegisterService;
  }

  public long bitwiseShiftCalculation(int testStationId, int startAddress, ModbusDevice modbusDevice, SubDevice subDevice) throws WaitingRoomException, ModbusTransportException, TimeoutException, ModbusDeviceException, InterruptedException {

    List<Map<String, Object>> registers = modbusRegisterService.getRegistersForTestStation(testStationId, startAddress, modbusDevice, subDevice);

    long result;
    if (registers.isEmpty()) {
      log.error("No registers available for device {} at start address {}. Possible closed WebSocket session or Modbus communication failure.",
              modbusDevice.getIpAddress(), startAddress);

      throw new IllegalStateException("No registers available for device " + modbusDevice.getIpAddress() +
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
    } else if (registers.size() == 2) {
      int register1 = (int) registers.get(0).get("value");
      int register2 = (int) registers.get(1).get("value");
      result = ((long) register1) | (long) register2 << 16;

    } else {
      log.error("Unexpected number of registers ({}) for bitwise shift calculation for device {} at address {}",
              registers.size(), modbusDevice.getIpAddress(), startAddress);
      throw new IllegalStateException(
              "Unexpected number of registers for bitwise shift calculation.");
    }
    return result;
  }
}


