package com.example.backend.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ModbusBitwiseService {
  private final ModbusRegisterService modbusRegisterService;

  public ModbusBitwiseService(ModbusRegisterService modbusRegisterService) {
    this.modbusRegisterService = modbusRegisterService;
  }


  public long bitwiseShiftCalculation(int deviceId,  int startAddress) {
    List<Map<String, Object>> registers = modbusRegisterService.getRegistersForDevice(deviceId, startAddress);

    if (registers.size() < 4) {
      throw new IllegalStateException("Not enough registers for bitwise shift calculation.");
    }
    int register1 = (int) registers.get(0).get("value");
    int register2 = (int) registers.get(1).get("value");
    int register3 = (int) registers.get(2).get("value");
    int register4 = (int) registers.get(3).get("value");

    return ((long) register1 << 48) | (long) register2 << 32 | (long) register3 << 16 | (long) register4;
  }
}
