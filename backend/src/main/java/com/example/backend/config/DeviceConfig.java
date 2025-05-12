package com.example.backend.config;


import com.example.backend.enums.BhkwCalculationType;
import com.example.backend.enums.EnergyCalculationType;
import com.example.backend.enums.GasCalculationType;
import com.example.backend.enums.HeatingCalculationType;
import com.example.backend.enums.SubDeviceType;
import com.example.backend.models.ModbusDevice;
import com.example.backend.models.SubDevice;
import com.example.backend.models.TestStation;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.*;


@Getter
@Component
public class DeviceConfig {
  private final List<TestStation> testStations = new ArrayList<>();


  @PostConstruct
  public void initialize() {
    System.out.println("Initializing devices...");

    SubDevice energy1 = new SubDevice(1000, 1, 4, SubDeviceType.ENERGY);
    energy1.addEnergyCalculationType(EnergyCalculationType.ERZEUGTE_ENERGIE);
    energy1.addEnergyCalculationType(EnergyCalculationType.GENUTZTE_ENERGIE);
    energy1.addEnergyCalculationType(EnergyCalculationType.WIRKLEISTUNG);
    energy1.addEnergyCalculationType(EnergyCalculationType.BLINDLEISTUNG_REACTIVPOWER);
    energy1.addEnergyCalculationType(EnergyCalculationType.SCHEINLEISTUNG_RESERVED);
    energy1.addEnergyCalculationType(EnergyCalculationType.SPANNUNG_L1_VOLTS);
    energy1.addEnergyCalculationType(EnergyCalculationType.SPANNUNG_L2_VOLTS);
    energy1.addEnergyCalculationType(EnergyCalculationType.SPANNUNG_L3_VOLTS);
    energy1.addEnergyCalculationType(EnergyCalculationType.FREQUENZ);
    energy1.addEnergyCalculationType(EnergyCalculationType.STROM);

    SubDevice energy2 = new SubDevice(2000, 1, 4, SubDeviceType.ENERGY);
    energy2.addEnergyCalculationType(EnergyCalculationType.ERZEUGTE_ENERGIE);
    energy2.addEnergyCalculationType(EnergyCalculationType.GENUTZTE_ENERGIE);
    energy2.addEnergyCalculationType(EnergyCalculationType.WIRKLEISTUNG);
    energy2.addEnergyCalculationType(EnergyCalculationType.BLINDLEISTUNG_REACTIVPOWER);
    energy2.addEnergyCalculationType(EnergyCalculationType.SCHEINLEISTUNG_RESERVED);
    energy2.addEnergyCalculationType(EnergyCalculationType.SPANNUNG_L1_VOLTS);
    energy2.addEnergyCalculationType(EnergyCalculationType.SPANNUNG_L2_VOLTS);
    energy2.addEnergyCalculationType(EnergyCalculationType.SPANNUNG_L3_VOLTS);
    energy2.addEnergyCalculationType(EnergyCalculationType.FREQUENZ);
    energy2.addEnergyCalculationType(EnergyCalculationType.STROM);

    SubDevice energy3 = new SubDevice(3000, 1, 4, SubDeviceType.ENERGY);
    energy3.addEnergyCalculationType(EnergyCalculationType.ERZEUGTE_ENERGIE);
    energy3.addEnergyCalculationType(EnergyCalculationType.GENUTZTE_ENERGIE);
    energy3.addEnergyCalculationType(EnergyCalculationType.WIRKLEISTUNG);
    energy3.addEnergyCalculationType(EnergyCalculationType.BLINDLEISTUNG_REACTIVPOWER);
    energy3.addEnergyCalculationType(EnergyCalculationType.SCHEINLEISTUNG_RESERVED);
    energy3.addEnergyCalculationType(EnergyCalculationType.SPANNUNG_L1_VOLTS);
    energy3.addEnergyCalculationType(EnergyCalculationType.SPANNUNG_L2_VOLTS);
    energy3.addEnergyCalculationType(EnergyCalculationType.SPANNUNG_L3_VOLTS);
    energy3.addEnergyCalculationType(EnergyCalculationType.FREQUENZ);
    energy3.addEnergyCalculationType(EnergyCalculationType.STROM);

    SubDevice heating2 = new SubDevice(12000, 1, 4, SubDeviceType.HEATING);
    heating2.addHeatingCalculationType(HeatingCalculationType.ERZEUGTE_ENERGIE_HEATING);
    heating2.addHeatingCalculationType(HeatingCalculationType.VORLAUFTEMPERATUR);
    heating2.addHeatingCalculationType(HeatingCalculationType.VOLUMENSTROM);
    heating2.addHeatingCalculationType(HeatingCalculationType.TEMPERATURDIFFERENZ);
    heating2.addHeatingCalculationType(HeatingCalculationType.LEISTUNG);
    heating2.addHeatingCalculationType(HeatingCalculationType.GESAMT_VOLUMEN);
    heating2.addHeatingCalculationType(HeatingCalculationType.RuCKLAUFTEMPERATUR);

    SubDevice heating3 = new SubDevice(13000, 1, 4, SubDeviceType.HEATING);
    heating3.addHeatingCalculationType(HeatingCalculationType.ERZEUGTE_ENERGIE_HEATING);
    heating3.addHeatingCalculationType(HeatingCalculationType.VORLAUFTEMPERATUR);
    heating3.addHeatingCalculationType(HeatingCalculationType.VOLUMENSTROM);
    heating3.addHeatingCalculationType(HeatingCalculationType.TEMPERATURDIFFERENZ);
    heating3.addHeatingCalculationType(HeatingCalculationType.LEISTUNG);
    heating3.addHeatingCalculationType(HeatingCalculationType.GESAMT_VOLUMEN);
    heating3.addHeatingCalculationType(HeatingCalculationType.RuCKLAUFTEMPERATUR);

    SubDevice gasTemperatur1 = new SubDevice(1, 1, 1, SubDeviceType.GAS);
    gasTemperatur1.addGasCalculationType(GasCalculationType.GAS_TEMPERATUR);
    gasTemperatur1.addGasCalculationType(GasCalculationType.GAS_ZAHLER);
    gasTemperatur1.addGasCalculationType(GasCalculationType.GAS_DRUCK);
    gasTemperatur1.addGasCalculationType(GasCalculationType.UMGEBUNGS_TEMPERATURE);
    gasTemperatur1.addGasCalculationType(GasCalculationType.UMGEBUNGS_DRUCK);


    SubDevice gasTemperatur2 = new SubDevice(3, 1, 1, SubDeviceType.GAS);
    gasTemperatur2.addGasCalculationType(GasCalculationType.GAS_TEMPERATUR);
    gasTemperatur2.addGasCalculationType(GasCalculationType.GAS_ZAHLER);
    gasTemperatur2.addGasCalculationType(GasCalculationType.GAS_DRUCK);
    gasTemperatur2.addGasCalculationType(GasCalculationType.UMGEBUNGS_TEMPERATURE);
    gasTemperatur2.addGasCalculationType(GasCalculationType.UMGEBUNGS_DRUCK);


    SubDevice gasTemperatur3 = new SubDevice(5, 1, 1, SubDeviceType.GAS);
    gasTemperatur3.addGasCalculationType(GasCalculationType.GAS_TEMPERATUR);
    gasTemperatur3.addGasCalculationType(GasCalculationType.GAS_ZAHLER);
    gasTemperatur3.addGasCalculationType(GasCalculationType.GAS_DRUCK);
    gasTemperatur3.addGasCalculationType(GasCalculationType.UMGEBUNGS_TEMPERATURE);
    gasTemperatur3.addGasCalculationType(GasCalculationType.UMGEBUNGS_DRUCK);

    SubDevice bhkw = new SubDevice(0, 1, 1, SubDeviceType.BHKW);
    bhkw.addBhkwCalculationType(BhkwCalculationType.ABGAS_TEMPERATUR);
    bhkw.addBhkwCalculationType(BhkwCalculationType.HEIZUNGS_WASSER_VORLAUF);
    bhkw.addBhkwCalculationType(BhkwCalculationType.HEIZUNGS_WASSER_RUCKLAUF);
    bhkw.addBhkwCalculationType(BhkwCalculationType. MOTOR_KUHLMITTEL_RUCKLAUF);
    bhkw.addBhkwCalculationType(BhkwCalculationType.MOTOR_KUHLMITTEL_VORLAUF);
    bhkw.addBhkwCalculationType(BhkwCalculationType.SCHALTSCHRANK);
    bhkw.addBhkwCalculationType(BhkwCalculationType.GEHAUSE);
    bhkw.addBhkwCalculationType(BhkwCalculationType.GENERATOR_WICKLUNG);
    bhkw.addBhkwCalculationType(BhkwCalculationType.MOTOR_OIL);



    SubDevice bhkw1 = new SubDevice(0, 1, 2, SubDeviceType.BHKW);
    bhkw1.addBhkwCalculationType(BhkwCalculationType.BETRIEBS_STUNDEN);
    bhkw1.addBhkwCalculationType(BhkwCalculationType.START_ANZAHL);




    // Create ModbusDevice (or Adapter) instances
    ModbusDevice device1 = new ModbusDevice("192.168.xxx.xx", 502, List.of(energy1));

    ModbusDevice device2 = new ModbusDevice("192.168.xxx.xx", 502,
        Arrays.asList(energy2, heating2));
    ModbusDevice device3 = new ModbusDevice("192.168.xxx.xx", 502,
        Arrays.asList(energy3, heating3));
    ModbusDevice device4 = new ModbusDevice("192.168.xxx.xx", 502,
        List.of(gasTemperatur1));
    ModbusDevice device5 = new ModbusDevice("192.168.xxx.xx", 502,
        List.of(gasTemperatur2));
    ModbusDevice device6 = new ModbusDevice("192.168.xxx.xx", 502,
        List.of(gasTemperatur3));
    ModbusDevice device7 = new ModbusDevice("192.168.xxx.xx", 502,
        Arrays.asList(bhkw,bhkw1));



    // Create TestStation instances
    TestStation testStation1 = new TestStation(1, "TestStation1", Arrays.asList(device1, device4, device7));
    TestStation testStation2 = new TestStation(2, "TestStation2", Arrays.asList(device2, device5, device7));
    TestStation testStation3 = new TestStation(3, "TestStation3", Arrays.asList(device3, device6, device7));

    // Add TestStations to the list
    testStations.add(testStation1);
    testStations.add(testStation2);
    testStations.add(testStation3);
  }
}