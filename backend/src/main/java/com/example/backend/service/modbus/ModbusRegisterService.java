package com.example.backend.service.modbus;

 import com.example.backend.config.socket.WebSocketHandlerCustom;
 import com.example.backend.exception.ModbusDeviceException;
 import com.example.backend.models.ModbusDevice;
 import com.example.backend.models.SubDevice;
 import com.example.backend.models.TestStation;
 import com.example.backend.service.teststation.TestStationService;
import com.serotonin.modbus4j.sero.messaging.TimeoutException;

import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.ExceptionCode;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomException;

import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ModbusRegisterService {

  private final TestStationService testStationService;
  private final ModbusClientService modbusClientService;
  private final WebSocketHandlerCustom webSocketHandlerCustom;


  public ModbusRegisterService(TestStationService testStationService, ModbusClientService modbusClientService, WebSocketHandlerCustom webSocketHandlerCustom) {
    this.testStationService = testStationService;
    this.modbusClientService = modbusClientService;
    this.webSocketHandlerCustom = webSocketHandlerCustom;
  }

  public List<Map<String, Object>> getRegistersForTestStation(int testStationId, int startAddress,
                                                              ModbusDevice modbusDevice, SubDevice subDevice)
          throws WaitingRoomException, ModbusTransportException, TimeoutException, ModbusDeviceException, InterruptedException {

    int slaveId = subDevice.getSlaveId();
    int registersQuantity = subDevice.getRegistersQuantity();
    List<Map<String, Object>> subDeviceRegisters;

    try {
      // Attempt to read the registers
      subDeviceRegisters = readHoldingRegisters(testStationId, slaveId, startAddress,
              registersQuantity, modbusDevice);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt(); // propagate interrupt status
      String errorMessage = "InterruptedException occurred while reading registers " + e.getMessage();
      sendWebSocketError(errorMessage);
      throw e;
    } catch (ModbusDeviceException e) {
      String errorMessage = "ModbusDeviceException occurred while reading registers " + e.getMessage();
      sendWebSocketError(errorMessage);
      throw e;
    } catch (ModbusTransportException e) {
      String errorMessage = "Modbus transport error occurred while reading registers: " + e.getMessage();
      sendWebSocketError(errorMessage);
      throw e;
    } catch (TimeoutException e) {
      String errorMessage = "TimeoutException occurred while reading registers: " + e.getMessage();
      sendWebSocketError(errorMessage);
      throw e;
    } catch (WaitingRoomException e) {
      String errorMessage = "WaitingRoomException occurred while reading registers";
      sendWebSocketError(errorMessage);
      throw e;
    }
    return new ArrayList<>(subDeviceRegisters);
  }


  private List<Map<String, Object>> readHoldingRegisters(int testStationId, int slaveId,
                                                         int startAddress, int quantity, ModbusDevice modbusDevice)
          throws WaitingRoomException, ModbusTransportException, TimeoutException, InterruptedException {

    List<Map<String, Object>> allProcessedRegisters = new ArrayList<>();

    List<ModbusMaster> modbusMasters = modbusClientService.getModbusMasterById(testStationId);

    if (modbusMasters == null || modbusMasters.isEmpty()) {
      String msg = "No ModbusMasters found for TestStation ID: " + testStationId;
      log.warn(msg);
      sendWebSocketError(msg);
      return Collections.emptyList();
    }
    TestStation testStation = testStationService.getTestStationById(testStationId);
    for (ModbusMaster mod : modbusMasters) {
      String ipAddress = modbusClientService.getIpAddressFromModbusMaster(mod, testStation);

      if (modbusDevice.getIpAddress().equals(ipAddress)) {
        boolean success = false;
        int retries = 2; // Retry 2 times before skipping the device

        // Retry logic
        while (retries > 0 && !success) {
          ReadHoldingRegistersRequest request = new ReadHoldingRegistersRequest(slaveId, startAddress, quantity);
          ReadHoldingRegistersResponse response = (ReadHoldingRegistersResponse) mod.send(request);
          if (response == null) {
            log.warn("No response received from Modbus device with IP: {}", ipAddress);
            retries--;
            if (retries == 0) {
              log.error("No response received after {} for the ip Address {}", retries, ipAddress);
              throw new ModbusDeviceException("Device " + ipAddress + " failed after " + retries);
            }
            continue;
          }

          if (response.isException()) {
            byte exceptionCode = response.getExceptionCode();
            String errorMessage = "Modbus Exception received from device with IP: " + ipAddress +
                    ", Exception Code: " + exceptionCode +
                    ", Error Message: " + ExceptionCode.getExceptionMessage(exceptionCode);
            log.warn(errorMessage);// 11, it indicates that the slave device failed to process the request, which is a general failure.
            if (exceptionCode == 11) {
              Map<String, Object> errorResponse = new HashMap<>();
              String retryMessage = "Device " + ipAddress + " failed to respond " + ", Exception Code: " + exceptionCode + ", Error Message: " + ExceptionCode.getExceptionMessage(exceptionCode) + ". Retrying or skipping." + " current retry " + retries + " start address is " + startAddress;
              log.warn(retryMessage);
              errorResponse.put("error", retryMessage);
              errorResponse.put("retry", true);
              webSocketHandlerCustom.pushDataToClients(errorResponse);
              retries--;
              if (retries == 0) {
                log.error("Device {} failed after retries.", ipAddress);
                throw new ModbusDeviceException("Device " + ipAddress + " failed after" + retries);
              }
              continue;  // Skip this iteration
            }
            sendWebSocketError(errorMessage);
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

  private void sendWebSocketError(String errorMessage) {
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", errorMessage);
    if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
      webSocketHandlerCustom.pushDataToClients(errorResponse);
    }
  }
}


