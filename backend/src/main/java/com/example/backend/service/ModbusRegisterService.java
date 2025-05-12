package com.example.backend.service;

import com.example.backend.config.WebSocketHandlerCustom;
import com.example.backend.enums.SubDeviceType;
import com.example.backend.exception.ModbusDeviceException;
import com.example.backend.models.ModbusDevice;
import com.example.backend.models.SubDevice;
import com.example.backend.models.TestStation;

import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.ExceptionCode;
import com.serotonin.modbus4j.exception.ModbusTransportException;

import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import com.serotonin.modbus4j.sero.messaging.TimeoutException;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class ModbusRegisterService {

  private final DeviceService deviceService;
  private final ModbusClientService modbusClientService;
  private final WebSocketHandlerCustom webSocketHandlerCustom;

  @Autowired
  public ModbusRegisterService(DeviceService deviceService,
      ModbusClientService modbusClientService, WebSocketHandlerCustom webSocketHandlerCustom) {
    this.deviceService = deviceService;
    this.modbusClientService = modbusClientService;


    this.webSocketHandlerCustom = webSocketHandlerCustom;
  }

  public List<Map<String, Object>> getRegistersForDevice(int testStationId, int startAddress,
      ModbusDevice modbusDevice, SubDeviceType subDeviceType)
      throws WaitingRoomException, ModbusTransportException, TimeoutException, ModbusDeviceException {

    List<Map<String, Object>> allRegisters = new ArrayList<>();

    for (SubDevice subDevice : modbusDevice.getSubDevices()) {
      if (subDevice.getType().equals(subDeviceType)) {
        int slaveId = subDevice.getSlaveId();
        int registersQuantity = subDevice.getRegistersQuantity();
        List<Map<String, Object>> subDeviceRegisters = null;

        try {
          // Attempt to read the registers
          subDeviceRegisters = readHoldingRegisters(testStationId, slaveId, startAddress,
              registersQuantity, modbusDevice);
        } catch (ModbusDeviceException e) {
          String errorMessage = "ModbusDeviceException occurred while reading registers";
          System.err.println(errorMessage + e.getMessage());
          Map<String, String> errorResponse = new HashMap<>();
          errorResponse.put("error", errorMessage);
          webSocketHandlerCustom.pushDataToClients(errorResponse);
          throw e;
        } catch (ModbusTransportException e) {
          String errorMessage = "Modbus transport error occurred while reading registers: " + e.getMessage();
          System.err.println(errorMessage);
          Map<String, String> errorResponse = new HashMap<>();
          errorResponse.put("error", errorMessage);
          webSocketHandlerCustom.pushDataToClients(errorResponse);
          throw e;
        } catch (TimeoutException e) {
          String errorMessage = "TimeoutException occurred while reading registers: " + e.getMessage();
          System.err.println(errorMessage + e.getMessage());
          Map<String, String> errorResponse = new HashMap<>();
          errorResponse.put("error", errorMessage);
          webSocketHandlerCustom.pushDataToClients(errorResponse);
          throw e;
        } catch (WaitingRoomException e) {
          String errorMessage = "WaitingRoomException occurred while reading registers";
          System.err.println(errorMessage + e.getMessage());
          Map<String, String> errorResponse = new HashMap<>();
          errorResponse.put("error", errorMessage);
          webSocketHandlerCustom.pushDataToClients(errorResponse);
          throw e;
        }

        allRegisters.addAll(subDeviceRegisters);
      }
    }

    return allRegisters;
  }


  private List<Map<String, Object>> readHoldingRegisters(int testStationId, int slaveId,
      int startAddress, int quantity, ModbusDevice modbusDevice)
      throws WaitingRoomException, ModbusTransportException, TimeoutException {

    List<Map<String, Object>> allProcessedRegisters = new ArrayList<>();
    List<ModbusMaster> modbusMasters = modbusClientService.getModbusMasterbyId(testStationId);
    TestStation testStation = deviceService.getTestStationById(testStationId);
    for (ModbusMaster mod : modbusMasters) {
      String ipAddress = modbusClientService.getIpAddressFromModbusMaster(mod, testStation);

      if (modbusDevice.getIpAddress().equals(ipAddress)) {
        boolean success = false;
        int retries = 2; // Retry 2 times before skipping the device

        // Retry logic
        while (retries > 0 && !success) {

          long startTime = System.currentTimeMillis();
          ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, startAddress, quantity);
          ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) mod.send(request);
          long endTime = System.currentTimeMillis();

          System.out.println("Modbus Response Time: " + (endTime - startTime) + " ms" + " for start Address " + startAddress);

          if (response == null) {
            System.err.println("No response received from Modbus device with IP: " + ipAddress);
            retries--;
            if (retries == 0) {
              System.err.println("Device " + ipAddress + " failed after retries.");
              throw new ModbusDeviceException("Device " + ipAddress + " failed after" + retries);
            }
            continue;
          }

          if (response.isException()) {
            Map<String, String> errorResponse = new HashMap<>();
            byte exceptionCode = response.getExceptionCode();
            String errorMessage = "Modbus Exception received from device with IP: " + ipAddress +
                ", Exception Code: " + exceptionCode +
                ", Error Message: " + ExceptionCode.getExceptionMessage(exceptionCode);

            System.err.println(errorMessage);// 11, it indicates that the slave device failed to process the request, which is a general failure.
            if (exceptionCode == 11) {
              String message = "Device " + ipAddress + " failed to respond " + ", Exception Code: " + exceptionCode + ", Error Message: " + ExceptionCode.getExceptionMessage(exceptionCode) + ". Retrying or skipping." + " current retry " + retries + " start address is " + startAddress;
              System.err.println(message);
              errorResponse.put("error", message);
              webSocketHandlerCustom.pushDataToClients(errorResponse);
              retries--;
              if (retries == 0) {
                System.err.println("Device " + ipAddress + " failed after retries.");
                throw new ModbusDeviceException("Device " + ipAddress + " failed after" + retries);
              }

              continue;  // Skip this iteration
            }

            errorResponse.put("error", errorMessage);
            webSocketHandlerCustom.pushDataToClients(errorResponse);
            retries = 0;  // Give up on retries if it's another exception
            continue;
          }


          short[] shortData = response.getShortData();
          for (int i = 0; i < shortData.length; i++) {
            Map<String, Object> register = new HashMap<>();
            register.put("address", startAddress + i);
            register.put("value", shortData[i] & 0xFFFF); // Convert to unsigned value
            allProcessedRegisters.add(register);
          }
          success = true;  // Exit retry loop after success
        }
      }
    }
    return allProcessedRegisters;
  }
}