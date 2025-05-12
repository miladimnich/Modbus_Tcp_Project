package com.example.backend.service;

import static com.example.backend.enums.BhkwCalculationType.BETRIEBS_STUNDEN;
import static com.example.backend.enums.BhkwCalculationType.START_ANZAHL;
import static com.example.backend.enums.EnergyCalculationType.ERZEUGTE_ENERGIE;
import static com.example.backend.enums.EnergyCalculationType.WIRKLEISTUNG;
import static com.example.backend.enums.GasCalculationType.GAS_ZAHLER;
import static com.example.backend.enums.HeatingCalculationType.ERZEUGTE_ENERGIE_HEATING;

import com.example.backend.config.MeasurementSessionRegistry;
import com.example.backend.config.ModbusPollingService;
import com.example.backend.models.ProductStatus;
import com.example.backend.models.Werksprotokolle;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.WerksprotokolleRepository;
import com.example.backend.service.BhkwService;
import com.example.backend.service.EnergyService;
import com.example.backend.service.GasService;
import com.example.backend.service.HeatingService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WerksprotokolleService {

  @Autowired
  WerksprotokolleRepository werksprotokolleRepository;
  @Autowired
  ProductRepository productRepository;
  @Autowired
  GasService gasService;
  @Autowired
  ModbusPollingService modbusPollingService;
  @Autowired
  MeasurementSessionRegistry measurementSessionRegistry;
  @Autowired
  BhkwService bhkwService;
  @Autowired
  EnergyService energyService;
  @Autowired
  HeatingService heatingService;


  public void recordStart(Integer serienNummer, long startTime) {
    ProductStatus productOpt = productRepository.findBySerienNummer(serienNummer);


    Integer direktBezug1Nr = productOpt.getObjektNr();


    Werksprotokolle werksprotokolle = werksprotokolleRepository.findByDirektBezug1Nr(direktBezug1Nr);

    LocalDateTime currentTime = Instant.ofEpochMilli(startTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime();

    werksprotokolle.setMessungAnfangUhrzeit(currentTime);
    werksprotokolleRepository.save(werksprotokolle);
  }

  public void recordEnd(Integer serienNummer, long endTime) {
    ProductStatus productOpt = productRepository.findBySerienNummer(serienNummer);


    Integer direktBezug1Nr = productOpt.getObjektNr();


    Werksprotokolle werksprotokolle = werksprotokolleRepository.findByDirektBezug1Nr(direktBezug1Nr);

    LocalDateTime time = Instant.ofEpochMilli(endTime)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime();

    werksprotokolle.setMessungEndeUhrzeit(time);
    werksprotokolleRepository.save(werksprotokolle);
  }

  public Werksprotokolle recordData(int deviceId) {
    Integer serienNummer = measurementSessionRegistry.getSerienNummer(deviceId);
    ProductStatus product = productRepository.findBySerienNummer(serienNummer);
    Integer objectNr = product.getObjektNr();
    Werksprotokolle protokoll = werksprotokolleRepository.findByDirektBezug1Nr(objectNr);


    if (gasService.getNow() != null) {
      protokoll.setMessungAnfangUhrzeit(
          LocalDateTime.ofInstant(Instant.ofEpochMilli(gasService.getNow()), ZoneId.systemDefault()));
    } else {
      protokoll.setMessungAnfangWaerme(null);
    }


    if (modbusPollingService.getEndTime() != null) {
      protokoll.setMessungEndeUhrzeit(
          LocalDateTime.ofInstant(Instant.ofEpochMilli(modbusPollingService.getEndTime()), ZoneId.systemDefault()));
    } else {
      protokoll.setMessungAnfangWaerme(null);
    }


    if (bhkwService.getFirstResults().containsKey(BETRIEBS_STUNDEN.name())) {
      Object result = bhkwService.getFirstResults().get(BETRIEBS_STUNDEN.name());
      Integer value = null;
      if (result instanceof String) {
        try {
          value = (int) Double.parseDouble((String) result);
        } catch (NumberFormatException e) {
          // handle error
        }
      }
      protokoll.setMessungAnfangBetriebsstundenInSekunden(value);
    } else {
      protokoll.setMessungAnfangWaerme(null);
    }


    if (bhkwService.getFirstResults().containsKey(START_ANZAHL.name())) {
      Object result = bhkwService.getFirstResults().get(START_ANZAHL.name());
      Integer value = null;
      if (result instanceof String) {
        try {
          value = (int) Double.parseDouble((String) result);
        } catch (NumberFormatException e) {
          // handle error
        }
      }
      protokoll.setMessungAnfangStarts(value);
    } else {
      protokoll.setMessungAnfangWaerme(null);
    }

    if (energyService.getFirstEnergyResults().containsKey(ERZEUGTE_ENERGIE.name())) {
      Object result = energyService.getFirstEnergyResults().get(ERZEUGTE_ENERGIE.name());
      Double value = null;
      if (result instanceof String) {
        try {
          value = Double.parseDouble((String) result);
        } catch (NumberFormatException e) {
          // handle error
        }
      }
      protokoll.setMessungAnfangkWh(value);
    } else {
      protokoll.setMessungAnfangWaerme(null);
    }


    if (energyService.getFirstEnergyResults().containsKey(WIRKLEISTUNG.name())) {
      Object result = energyService.getFirstEnergyResults().get(WIRKLEISTUNG.name());
      Double value = null;
      if (result instanceof String) {
        try {
          value = Double.parseDouble((String) result);
        } catch (NumberFormatException e) {
          // handle error
        }
      }
      protokoll.setMessungAnfangLeistung(value);
    } else {
      protokoll.setMessungAnfangWaerme(null);
    }


    if (heatingService.getFirstHeatingResults().containsKey(ERZEUGTE_ENERGIE_HEATING.name())) {
      Object result = heatingService.getFirstHeatingResults().get(ERZEUGTE_ENERGIE_HEATING.name());
      Double value = null;
      if (result instanceof String) {
        try {
          value = Double.parseDouble((String) result);
        } catch (NumberFormatException e) {
          // handle error
        }
      }
      protokoll.setMessungAnfangWaerme(value);
    } else {
      protokoll.setMessungAnfangWaerme(null);
    }

    if (gasService.getFirstResults().containsKey(GAS_ZAHLER.name())) {
      protokoll.setMessungAnfangGas(0.0);
    }


    return werksprotokolleRepository.save(protokoll);
  }
}
