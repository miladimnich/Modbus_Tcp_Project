package com.example.backend.config;

import com.example.backend.enums.*;
import com.example.backend.models.ModbusDevice;
import com.example.backend.models.SubDevice;
import com.example.backend.models.TestStation;
import jakarta.annotation.PostConstruct;
 import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Getter
@Component
public class TestStationConfig {

    private final List<TestStation> testStations = new ArrayList<>();

    @PostConstruct
    public void initialize() {
        try {
            log.info("Initializing devices...");

            SubDevice energy1 = new SubDevice(1000, 1, 4, SubDeviceType.ENERGY);
            energy1.addEnergyCalculationType(EnergyCalculationType.GENERATED_ENERGY);
            energy1.addEnergyCalculationType(EnergyCalculationType.CONSUMED_ENERGY);
            energy1.addEnergyCalculationType(EnergyCalculationType.ACTIVE_POWER);
            energy1.addEnergyCalculationType(EnergyCalculationType.REACTIVE_POWER_BLIND_POWER);
            energy1.addEnergyCalculationType(EnergyCalculationType.APPARENT_POWER_RESERVED);
            energy1.addEnergyCalculationType(EnergyCalculationType.VOLTAGE_L1_VOLTS);
            energy1.addEnergyCalculationType(EnergyCalculationType.VOLTAGE_L2_VOLTS);
            energy1.addEnergyCalculationType(EnergyCalculationType.VOLTAGE_L3_VOLTS);
            energy1.addEnergyCalculationType(EnergyCalculationType.FREQUENCY);
            energy1.addEnergyCalculationType(EnergyCalculationType.CURRENT);

            SubDevice energy2 = new SubDevice(2000, 1, 4, SubDeviceType.ENERGY);
            energy2.addEnergyCalculationType(EnergyCalculationType.GENERATED_ENERGY);
            energy2.addEnergyCalculationType(EnergyCalculationType.CONSUMED_ENERGY);
            energy2.addEnergyCalculationType(EnergyCalculationType.ACTIVE_POWER);
            energy2.addEnergyCalculationType(EnergyCalculationType.REACTIVE_POWER_BLIND_POWER);
            energy2.addEnergyCalculationType(EnergyCalculationType.APPARENT_POWER_RESERVED);
            energy2.addEnergyCalculationType(EnergyCalculationType.VOLTAGE_L1_VOLTS);
            energy2.addEnergyCalculationType(EnergyCalculationType.VOLTAGE_L2_VOLTS);
            energy2.addEnergyCalculationType(EnergyCalculationType.VOLTAGE_L3_VOLTS);
            energy2.addEnergyCalculationType(EnergyCalculationType.FREQUENCY);
            energy2.addEnergyCalculationType(EnergyCalculationType.CURRENT);

            SubDevice energy3 = new SubDevice(3000, 1, 4, SubDeviceType.ENERGY);
            energy3.addEnergyCalculationType(EnergyCalculationType.GENERATED_ENERGY);
            energy3.addEnergyCalculationType(EnergyCalculationType.CONSUMED_ENERGY);
            energy3.addEnergyCalculationType(EnergyCalculationType.ACTIVE_POWER);
            energy3.addEnergyCalculationType(EnergyCalculationType.REACTIVE_POWER_BLIND_POWER);
            energy3.addEnergyCalculationType(EnergyCalculationType.APPARENT_POWER_RESERVED);
            energy3.addEnergyCalculationType(EnergyCalculationType.VOLTAGE_L1_VOLTS);
            energy3.addEnergyCalculationType(EnergyCalculationType.VOLTAGE_L2_VOLTS);
            energy3.addEnergyCalculationType(EnergyCalculationType.VOLTAGE_L3_VOLTS);
            energy3.addEnergyCalculationType(EnergyCalculationType.FREQUENCY);
            energy3.addEnergyCalculationType(EnergyCalculationType.CURRENT);


            SubDevice heating1 = new SubDevice(11000, 1, 4, SubDeviceType.HEATING);
            heating1.addHeatingCalculationType(HeatingCalculationType.GENERATED_ENERGY_HEATING); //10
            heating1.addHeatingCalculationType(HeatingCalculationType.SUPPLY_TEMPERATURE); //50
            heating1.addHeatingCalculationType(HeatingCalculationType.VOLUME_FLOW); //40
            heating1.addHeatingCalculationType(HeatingCalculationType.TEMPERATURE_DIFFERENCE); //70
            heating1.addHeatingCalculationType(HeatingCalculationType.TOTAL_VOLUME); //20
            heating1.addHeatingCalculationType(HeatingCalculationType.RETURN_TEMPERATURE); //60
            heating1.addHeatingCalculationType(HeatingCalculationType.POWER);


            SubDevice heating2 = new SubDevice(12000, 1, 4, SubDeviceType.HEATING);
            heating2.addHeatingCalculationType(HeatingCalculationType.GENERATED_ENERGY_HEATING);
            heating2.addHeatingCalculationType(HeatingCalculationType.SUPPLY_TEMPERATURE);
            heating2.addHeatingCalculationType(HeatingCalculationType.VOLUME_FLOW);
            heating2.addHeatingCalculationType(HeatingCalculationType.TEMPERATURE_DIFFERENCE);
            heating2.addHeatingCalculationType(HeatingCalculationType.POWER);
            heating2.addHeatingCalculationType(HeatingCalculationType.TOTAL_VOLUME); //20
            heating2.addHeatingCalculationType(HeatingCalculationType.RETURN_TEMPERATURE);

            SubDevice heating3 = new SubDevice(13000, 1, 4, SubDeviceType.HEATING);
            heating3.addHeatingCalculationType(HeatingCalculationType.GENERATED_ENERGY_HEATING);
            heating3.addHeatingCalculationType(HeatingCalculationType.SUPPLY_TEMPERATURE);
            heating3.addHeatingCalculationType(HeatingCalculationType.VOLUME_FLOW);
            heating3.addHeatingCalculationType(HeatingCalculationType.TEMPERATURE_DIFFERENCE);
            heating3.addHeatingCalculationType(HeatingCalculationType.POWER);
            heating3.addHeatingCalculationType(HeatingCalculationType.TOTAL_VOLUME);
            heating3.addHeatingCalculationType(HeatingCalculationType.RETURN_TEMPERATURE);

            SubDevice gasTemperature1 = new SubDevice(1, 1, 1, SubDeviceType.GAS);
            gasTemperature1.addGasCalculationType(GasCalculationType.GAS_TEMPERATURE);
            gasTemperature1.addGasCalculationType(GasCalculationType.GAS_METER);
            gasTemperature1.addGasCalculationType(GasCalculationType.AMBIENT_TEMPERATURE);
            gasTemperature1.addGasCalculationType(GasCalculationType.AMBIENT_PRESSURE);
            gasTemperature1.addGasCalculationType(GasCalculationType.GAS_PRESSURE);


            SubDevice gasTemperature2 = new SubDevice(3, 1, 1, SubDeviceType.GAS);
            gasTemperature2.addGasCalculationType(GasCalculationType.GAS_TEMPERATURE);
            gasTemperature2.addGasCalculationType(GasCalculationType.GAS_METER);
            gasTemperature2.addGasCalculationType(GasCalculationType.AMBIENT_TEMPERATURE);
            gasTemperature2.addGasCalculationType(GasCalculationType.AMBIENT_PRESSURE);
            gasTemperature2.addGasCalculationType(GasCalculationType.GAS_PRESSURE);


            SubDevice gasTemperature3 = new SubDevice(5, 1, 1, SubDeviceType.GAS);
            gasTemperature3.addGasCalculationType(GasCalculationType.GAS_TEMPERATURE);
            gasTemperature3.addGasCalculationType(GasCalculationType.GAS_METER);
            gasTemperature3.addGasCalculationType(GasCalculationType.AMBIENT_TEMPERATURE);
            gasTemperature3.addGasCalculationType(GasCalculationType.AMBIENT_PRESSURE);
            gasTemperature3.addGasCalculationType(GasCalculationType.GAS_PRESSURE);


            SubDevice chp1 = new SubDevice(0, 1, 1, SubDeviceType.CHP);
            chp1.addChpCalculationType(ChpCalculationType.EXHAUST_TEMPERATURE);
            chp1.addChpCalculationType(ChpCalculationType.HEATING_WATER_FLOW);
            chp1.addChpCalculationType(ChpCalculationType.HEATING_WATER_RETURN);
            chp1.addChpCalculationType(ChpCalculationType.ENGINE_COOLANT_RETURN);
            chp1.addChpCalculationType(ChpCalculationType.ENGINE_COOLANT_FLOW);
            chp1.addChpCalculationType(ChpCalculationType.CONTROL_CABINET);
            chp1.addChpCalculationType(ChpCalculationType.HOUSING);
            chp1.addChpCalculationType(ChpCalculationType.GENERATOR_WINDING);
            chp1.addChpCalculationType(ChpCalculationType.ENGINE_OIL);
            chp1.addChpCalculationType(ChpCalculationType.START_COUNT);
            chp1.addChpCalculationType(ChpCalculationType.ENGINE_COOLANT);


            SubDevice chp2 = new SubDevice(0, 1, 2, SubDeviceType.CHP);
            chp2.addChpCalculationType(ChpCalculationType.OPERATING_HOURS);


            // Create ModbusDevice (or Adapter) instances
            ModbusDevice device1 = new ModbusDevice("192.168.xxx.x7", 502,
                    Arrays.asList(energy1, heating1));
            ModbusDevice device2 = new ModbusDevice("192.168.xxx.x7", 502,
                    Arrays.asList(energy2, heating2));
            ModbusDevice device3 = new ModbusDevice("192.168.xxx.x7", 502,
                    Arrays.asList(energy3, heating3));
            ModbusDevice device4 = new ModbusDevice("192.168.xxx.x8", 502,
                    List.of(gasTemperature1));
            ModbusDevice device5 = new ModbusDevice("192.168.xxx.x8", 502,
                    List.of(gasTemperature2));
            ModbusDevice device6 = new ModbusDevice("192.168.xxx.x8", 502,
                    List.of(gasTemperature3));
            ModbusDevice device7 = new ModbusDevice("192.168.xxx.x9", 502,
                    Arrays.asList(chp1, chp2));


            // Create TestStation instances
            TestStation testStation1 = new TestStation(1, "Prüfstand 1", Arrays.asList(device1, device4, device7));
            TestStation testStation2 = new TestStation(2, "Prüfstand 2", Arrays.asList(device2, device5, device7));
            TestStation testStation3 = new TestStation(3, "Prüfstand 3", Arrays.asList(device3, device6, device7));

            // Add TestStations to the list
            testStations.add(testStation1);
            testStations.add(testStation2);
            testStations.add(testStation3);

            log.info("Device initialization complete. {} test stations loaded.", testStations.size());
            for (TestStation ts : testStations) {
                log.debug("TestStation ID: {}, Name: {}, ModbusDevices: {}", ts.getId(), ts.getTestStationName(), ts.getModbusDevices().size());
            }
        } catch (Exception e) {
            log.error("Error during device initialization", e);
        }
    }
}