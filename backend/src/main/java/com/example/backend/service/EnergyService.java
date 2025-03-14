package com.example.backend.service;

import com.example.backend.config.WebSocketHandlerCustom;
import com.example.backend.enums.EnergyCalculationType;
import com.example.backend.enums.SubDeviceType;
import com.example.backend.models.Device;
import com.example.backend.models.SubDevice;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EnergyService {
  @Getter
  private final Map<String, Long> firstEnergyResults = new ConcurrentHashMap<>();
  @Getter
  private final Map<String, Long> currentEnergyResults = new ConcurrentHashMap<>();

  @Getter
  private final Map<String, Long> lastEnergyResults = new ConcurrentHashMap<>();


  private final ModbusBitwiseService modbusBitwiseService;
  private final DeviceService deviceService;
  private final WebSocketHandlerCustom webSocketHandlerCustom;
  //  private final ApplicationEventPublisher eventPublisher;

  @Autowired
  public EnergyService(ModbusBitwiseService modbusBitwiseService, DeviceService deviceService, WebSocketHandlerCustom webSocketHandlerCustom) {
    this.modbusBitwiseService = modbusBitwiseService;
    this.deviceService = deviceService;
    this.webSocketHandlerCustom = webSocketHandlerCustom;

  }

  public long calculateErzeugteEnergie(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 10);
  }

  public long calculateGenutzteEnergie(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 20);
  }

  public long calculateWirkLeistungPower(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 30);
  }

  public long calculateBlindLeistungReactivePower(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 40);
  }

  public long calculateScheinLeistungReserved(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 50);
  }

  public long calculateSpannungL1Volts(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 60);
  }

  public long calculateSpannungL2Volts(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 70);
  }

  public long calculateSpannungL3Volts(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 80);
  }

  public long calculateFrequentFrequency(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 90);
  }

  public long calculateStormAmpere(int deviceId, int startAddress) {
    return modbusBitwiseService.bitwiseShiftCalculation(deviceId, startAddress + 100);
  }


  public void processEnergyData(int deviceId) {
    Device device = deviceService.getDeviceById(deviceId);
    for (SubDevice subDevice : device.getSubDevices()) {
      int startAddress = subDevice.getStartAddress();
      if (subDevice.getType() == SubDeviceType.ENERGY) {
        System.out.println("Processing energy data for deviceId: " + deviceId); // Add logging here
        processAndPush(deviceId, EnergyCalculationType.ERZEUGTE_ENERGIE.name(), calculateErzeugteEnergie(deviceId, startAddress));
        processAndPush(deviceId, EnergyCalculationType.GENUTZTE_ENERGIE.name(), calculateGenutzteEnergie(deviceId, startAddress));
        processAndPush(deviceId, EnergyCalculationType.WIRKLEISTUNG.name(), calculateWirkLeistungPower(deviceId, startAddress));
        processAndPush(deviceId, EnergyCalculationType.BLINDLEISTUNG_REACTIVPOWER.name(), calculateBlindLeistungReactivePower(deviceId, startAddress));
        processAndPush(deviceId, EnergyCalculationType.SCHEINLEISTUNG_RESERVED.name(), calculateScheinLeistungReserved(deviceId, startAddress));
        processAndPush(deviceId, EnergyCalculationType.SPANNUNG_L1_VOLTS.name(), calculateSpannungL1Volts(deviceId, startAddress));
        processAndPush(deviceId, EnergyCalculationType.SPANNUNG_L2_VOLTS.name(), calculateSpannungL2Volts(deviceId, startAddress));
        processAndPush(deviceId, EnergyCalculationType.SPANNUNG_L3_VOLTS.name(), calculateSpannungL3Volts(deviceId, startAddress));
        processAndPush(deviceId, EnergyCalculationType.FREQUENZ.name(), calculateFrequentFrequency(deviceId, startAddress));
        processAndPush(deviceId, EnergyCalculationType.STROM.name(), calculateStormAmpere(deviceId, startAddress));
      }
    }

  }

  public void processAndPush(int deviceId, String key, long value) {
    if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
      boolean valueChanged = false;
      currentEnergyResults.put(key, value);
      if (lastEnergyResults.get(key) == null) {
        lastEnergyResults.put(key, value);
        valueChanged = true;
      }

      if (!currentEnergyResults.get(key).equals(lastEnergyResults.get(key))) {
        lastEnergyResults.put(key, value);
        currentEnergyResults.put(key, value);
        valueChanged = true;
      }

      if (valueChanged) {
        Map<String, Object> update = new LinkedHashMap<>();
        update.put(key, currentEnergyResults.get(key));
        update.put("deviceId", deviceId);
        webSocketHandlerCustom.pushDataToClients(update);
      }
    }

  }


  public Map<String, Long> startResults(int deviceId) {
    firstEnergyResults.putAll(currentEnergyResults);
    return firstEnergyResults;
  }

  public Map<String, Long> lastResults(int deviceId) {
    return lastEnergyResults;
  }
}
