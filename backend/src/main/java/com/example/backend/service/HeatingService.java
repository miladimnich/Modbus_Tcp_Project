package com.example.backend.service;

import com.example.backend.config.WebSocketHandlerCustom;
import com.example.backend.enums.HeatingCalculationType;
import com.example.backend.enums.SubDeviceType;
import com.example.backend.models.Device;
import com.example.backend.models.SubDevice;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
public class HeatingService {

  @Getter
  private final Map<String, Long> firstHeatingResults = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Long> currentHeatingResults = new ConcurrentHashMap<>();

  @Getter
  private final Map<String, Long> lastHeatingResults = new ConcurrentHashMap<>();

  private final WebSocketHandlerCustom webSocketHandlerCustom;


  private final ModbusBitwiseService modbusBitwiseService;
  private final DeviceService deviceService;

  public HeatingService(WebSocketHandlerCustom webSocketHandlerCustom, ModbusBitwiseService modbusBitwiseService, DeviceService deviceService) {
    this.webSocketHandlerCustom = webSocketHandlerCustom;
    this.modbusBitwiseService = modbusBitwiseService;
    this.deviceService = deviceService;
  }

  public long calculateErzeugteEnergie(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 10);

  }

  public long calculateGesamtVolumen(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 20);
  }

  public long calculateLeistung(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 30);
  }

  public long calculateVolumenstrom(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 40);
  }

  public long calculateVorlaufTemperatur(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 50);
  }

  public long calculateRuckLaufTemperatur(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 60);
  }

  public long calculateTemperaturDifference(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 70);
  }

  public void processHeatingData(int deviceId) {
    Device device = deviceService.getDeviceById(deviceId);
    for (SubDevice subDevice : device.getSubDevices()) {
      int startAddress = subDevice.getStartAddress();
      if (subDevice.getType() == SubDeviceType.HEATING) {
        System.out.println("Processing heating data for deviceId: " + deviceId); // Add logging here
        processAndPush(deviceId, HeatingCalculationType.ERZEUGTE_ENERGIE_HEATING.name(), calculateErzeugteEnergie(deviceId, startAddress));
        processAndPush(deviceId, HeatingCalculationType.GESAMT_VOLUMEN.name(), calculateGesamtVolumen(deviceId, startAddress));
        processAndPush(deviceId, HeatingCalculationType.LEISTUNG.name(), calculateLeistung(deviceId, startAddress));
        processAndPush(deviceId, HeatingCalculationType.VOLUMENSTROM.name(), calculateVolumenstrom(deviceId, startAddress));
        processAndPush(deviceId, HeatingCalculationType.VORLAUFTEMPERATUR.name(), calculateVorlaufTemperatur(deviceId, startAddress));
        processAndPush(deviceId, HeatingCalculationType.RuCKLAUFTEMPERATUR.name(), calculateRuckLaufTemperatur(deviceId, startAddress));
        processAndPush(deviceId, HeatingCalculationType.TEMPERATURDIFFERENZ.name(), calculateTemperaturDifference(deviceId, startAddress));

      }
    }
  }

  public void processAndPush(int deviceId, String key, long value) {
    if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
      boolean valueChanged = false;
      currentHeatingResults.put(key, value);
      if (lastHeatingResults.get(key) == null) {
        lastHeatingResults.put(key, value);
        valueChanged = true;
      }

      if (!currentHeatingResults.get(key).equals(lastHeatingResults.get(key))) {
        lastHeatingResults.put(key, value);
        currentHeatingResults.put(key, value);
        valueChanged = true;
      }

      if (valueChanged) {
        Map<String, Object> update = new LinkedHashMap<>();
        update.put(key, currentHeatingResults.get(key));
        update.put("deviceId", deviceId);
        webSocketHandlerCustom.pushDataToClients(update);

      }
    }

  }

  public Map<String, Long> startResults(int deviceId) {
    firstHeatingResults.putAll(currentHeatingResults);
    return firstHeatingResults;
  }

  public Map<String, Long> lastResults(int deviceId) {
    return lastHeatingResults;
  }


}
