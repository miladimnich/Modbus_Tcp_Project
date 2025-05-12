package com.example.backend.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class MeasurementSessionRegistry {

  private final Map<Integer, Integer> deviceToSerienNummer = new ConcurrentHashMap<>();

  public void registerSerienNummer(int deviceId, int serienNummer) {
    deviceToSerienNummer.put(deviceId, serienNummer);
  }
  public Integer getSerienNummer(int deviceId){
    return  deviceToSerienNummer.get(deviceId);
  }


}