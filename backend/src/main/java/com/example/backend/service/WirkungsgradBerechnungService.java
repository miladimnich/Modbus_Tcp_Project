package com.example.backend.service;

import com.example.backend.config.WebSocketHandlerCustom;
import com.example.backend.enums.GasData;
import com.example.backend.events.GasCalculationCompleteEvent;
import com.example.backend.service.EnergyService;
import com.example.backend.service.GasService;
import com.example.backend.service.HeatingService;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class WirkungsgradBerechnungService {
  private final WebSocketHandlerCustom webSocketHandlerCustom;
  private final GasService gasService;
  private final EnergyService energyService;
  private final HeatingService heatingService;



  public WirkungsgradBerechnungService(WebSocketHandlerCustom webSocketHandlerCustom, GasService gasService, EnergyService energyService, HeatingService heatingService) {
    this.webSocketHandlerCustom = webSocketHandlerCustom;
    this.gasService = gasService;
    this.energyService = energyService;
    this.heatingService = heatingService;
  }

  @EventListener
  @Async
  public void handleGasCalculationCompleteEvent(GasCalculationCompleteEvent event) throws InterruptedException {
    int deviceId = event.getDeviceId();
    berechnungElektrischerWirkungsgrad(deviceId);
    berechnungThermischerWirkungsgrad(deviceId);
    berechnungGesamtWirkungsGrad(deviceId);
  }


  public void berechnungElektrischerWirkungsgrad(int deviceId) {


    Double wirkLeistungValue = energyService.getEnergyDifference();
    Double gasLeistung = gasService.getGasLeistungResult();

    if (wirkLeistungValue.isNaN() || gasLeistung.isNaN()) {
      System.out.println("WirkLeistung is NaN");
      return;
    }

    double result = (wirkLeistungValue / gasLeistung) * 100;

    String formattedResult = String.format(Locale.US, "%.2f", result);
    Map<String, Object> update = new LinkedHashMap<>();
    update.put(GasData.ELEKTRISCHER_WIRKUNGSGRAD.name(), formattedResult);
    update.put("deviceId", deviceId);
    System.out.println("ELEKTRISCHER WIRKUNGSGRAD before pushing " + result);
    webSocketHandlerCustom.enqueueUpdate(update);
  }


  public void berechnungGesamtWirkungsGrad(int deviceId) throws InterruptedException {
    Double wirkleistung = energyService.getEnergyDifference();
    Double gasLeistung = gasService.getGasLeistungResult();
    Double heatingLeistung = heatingService.getHeatingDifference();

    if (wirkleistung.isNaN() || gasLeistung.isNaN() || heatingLeistung.isNaN()) {
      return;
    }

    Double result = (wirkleistung + heatingLeistung) / gasLeistung;
    String formattedResult = String.format(Locale.US, "%.2f", result);
    Map<String, Object> update = new LinkedHashMap<>();
    update.put(GasData.GESAMT_WIRKUNGSGRAD.name(), formattedResult);
    update.put("deviceId", deviceId);
    webSocketHandlerCustom.enqueueUpdate(update);
  }

  public void berechnungThermischerWirkungsgrad(int deviceId) throws InterruptedException {
    if (!heatingService.hasHeatingSubDevice(deviceId)) {
      System.out.println("Skipping thermischer Wirkungsgrad calculation: No HEATING subdevice for device " + deviceId);
      return;
    }

    heatingService.processHeatingData(deviceId);
    heatingService.calculateAndPushHeatingDifference(deviceId);

    Double heatingLeistung = heatingService.getHeatingDifference();
    Double gasLeistung = gasService.getGasLeistungResult();

    if (gasLeistung.isNaN() || heatingLeistung.isNaN()) {
      return;
    }

    Double result = heatingLeistung / gasLeistung;
    String formattedResult = String.format(Locale.US, "%.2f", result);
    Map<String, Object> update = new LinkedHashMap<>();
    update.put(GasData.THERMISCHER_WIRKUNGSGRAD.name(), formattedResult);
    update.put("deviceId", deviceId);
    webSocketHandlerCustom.enqueueUpdate(update);
  }

}