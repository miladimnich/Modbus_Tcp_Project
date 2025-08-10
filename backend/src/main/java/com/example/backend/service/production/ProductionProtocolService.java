package com.example.backend.service.production;

import com.example.backend.config.MeasurementSessionRegistry;
import com.example.backend.models.ProductStatus;
import com.example.backend.models.ProductionProtocol;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductionProtocolRepository;
import com.example.backend.service.chp.ChpService;
import com.example.backend.service.energy.EnergyService;
import com.example.backend.service.gas.GasService;
import com.example.backend.service.heating.HeatingService;
import com.example.backend.service.polling.PollingControlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.example.backend.enums.ChpCalculationType.*;
import static com.example.backend.enums.EnergyCalculationType.ACTIVE_POWER;
import static com.example.backend.enums.EnergyCalculationType.GENERATED_ENERGY;
import static com.example.backend.enums.GasCalculationType.*;
import static com.example.backend.enums.HeatingCalculationType.*;

@Slf4j
@Service
public class ProductionProtocolService {

    private final ProductionProtocolRepository productionProtocolRepository;
    private final ProductRepository productRepository;
    private final GasService gasService;
    private final MeasurementSessionRegistry measurementSessionRegistry;
    private final ChpService chpService;
    private final EnergyService energyService;
    private final HeatingService heatingService;
    private final PollingControlService pollingControlService;

    public ProductionProtocolService(ProductionProtocolRepository productionProtocolRepository, ProductRepository productRepository, GasService gasService, MeasurementSessionRegistry measurementSessionRegistry, ChpService chpService, EnergyService energyService, HeatingService heatingService, PollingControlService pollingControlService) {
        this.productionProtocolRepository = productionProtocolRepository;
        this.productRepository = productRepository;
        this.gasService = gasService;
        this.measurementSessionRegistry = measurementSessionRegistry;
        this.chpService = chpService;
        this.energyService = energyService;
        this.heatingService = heatingService;
        this.pollingControlService = pollingControlService;
    }


    public ProductionProtocol recordData() {
        Integer serialNumber = measurementSessionRegistry.getSerialNumber();
        ProductStatus product = productRepository.findBySerialNumber(serialNumber);
        Integer objectNr = product.getObjectNumber(); //make check if its exist
        ProductionProtocol protocol = productionProtocolRepository.findByDirectReference1Number(objectNr);

        if (serialNumber == null || objectNr == null) {
            return null;
        }
        if (gasService.getTimestampNow() >= 0) {
            protocol.setMessungAnfangUhrzeit(
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(gasService.getTimestampNow()), ZoneId.systemDefault()));
        } else {
            protocol.setMessungAnfangUhrzeit(null);
        }


        if (pollingControlService.getEndTime() != -1) {
            protocol.setMessungEndeUhrzeit(
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(pollingControlService.getEndTime()), ZoneId.systemDefault()));
        } else {
            protocol.setMessungEndeUhrzeit(null);
        }


        if (chpService.getInitialChpResults().containsKey(OPERATING_HOURS.name())) {
            Object result = chpService.getInitialChpResults().get(OPERATING_HOURS.name());
            Integer value = null;
            if (result instanceof Long) {
                try {
                    value = ((Long) result).intValue();
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangBetriebsstundenInSekunden(value);
        } else {
            protocol.setMessungAnfangBetriebsstundenInSekunden(null);
        }


        if (chpService.getInitialChpResults().containsKey(START_COUNT.name())) {
            Object result = chpService.getInitialChpResults().get(START_COUNT.name());
            Integer value = null;
            if (result instanceof Long) {
                try {
                    value = ((Long) result).intValue();
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangStarts(value);
        } else {
            protocol.setMessungAnfangStarts(null);
        }

        if (energyService.getInitialEnergyResults().containsKey(GENERATED_ENERGY.name())) {
            Object result = energyService.getInitialEnergyResults().get(GENERATED_ENERGY.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangkWh(value);
        } else {
            protocol.setMessungAnfangkWh(null);
        }


        if (energyService.getInitialEnergyResults().containsKey(ACTIVE_POWER.name())) {
            Object result = energyService.getInitialEnergyResults().get(ACTIVE_POWER.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangLeistung(value);
        } else {
            protocol.setMessungAnfangLeistung(null);
        }


        if (heatingService.getInitialHeatingResults().containsKey(GENERATED_ENERGY_HEATING.name())) {
            Object result = heatingService.getInitialHeatingResults().get(GENERATED_ENERGY_HEATING.name());
            Double value = null;
            if (result instanceof Long) {
                try {
                    value = ((Long) result).doubleValue();
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangWaerme(value);
        } else {
            protocol.setMessungAnfangWaerme(null);
        }


        if (gasService.getInitialGasResults().containsKey(GAS_METER.name())) {
            protocol.setMessungAnfangGas(0.0);
        } else {
            protocol.setMessungAnfangGas(null);
        }


        if (gasService.getInitialGasResults().containsKey(GAS_TEMPERATURE.name())) {
            Object result = gasService.getInitialGasResults().get(GAS_TEMPERATURE.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangGastemperatur(value);
        } else {
            protocol.setMessungAnfangGastemperatur(null);
        }


        if (gasService.getInitialGasResults().containsKey(GAS_PRESSURE.name())) {
            Object result = gasService.getInitialGasResults().get(GAS_PRESSURE.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangGasfliessdruck(value);
        } else {
            protocol.setMessungAnfangGasfliessdruck(null);
        }

        if (gasService.getInitialGasResults().containsKey(AMBIENT_PRESSURE.name())) {
            Object result = gasService.getInitialGasResults().get(AMBIENT_PRESSURE.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangLuftdruck(value);
        } else {
            protocol.setMessungAnfangLuftdruck(null);
        }


        if (gasService.getInitialGasResults().containsKey(AMBIENT_TEMPERATURE.name())) {
            Object result = gasService.getInitialGasResults().get(AMBIENT_TEMPERATURE.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangLufttemperatur(value);
        } else {
            protocol.setMessungAnfangLufttemperatur(null);
        }

        if (heatingService.getInitialHeatingResults().containsKey(RETURN_TEMPERATURE.name())) {
            Object result = heatingService.getInitialHeatingResults().get(RETURN_TEMPERATURE.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangTemp1WMZ(value);
        } else {
            protocol.setMessungAnfangTemp1WMZ(null);
        }

        if (heatingService.getInitialHeatingResults().containsKey(SUPPLY_TEMPERATURE.name())) {
            Object result = heatingService.getInitialHeatingResults().get(SUPPLY_TEMPERATURE.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangTemp2WMZ(value);
        } else {
            protocol.setMessungAnfangTemp2WMZ(null);
        }

        if (heatingService.getInitialHeatingResults().containsKey(VOLUME_FLOW.name())) {
            Object result = heatingService.getInitialHeatingResults().get(VOLUME_FLOW.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangVolumenstromWMZ(value);
        } else {
            protocol.setMessungAnfangVolumenstromWMZ(null);
        }

        if (chpService.getInitialChpResults().containsKey(HEATING_WATER_RETURN.name())) {
            Object result = chpService.getInitialChpResults().get(HEATING_WATER_RETURN.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangTemp1(value);
        } else {
            protocol.setMessungAnfangTemp1(null);
        }

        if (chpService.getInitialChpResults().containsKey(HEATING_WATER_FLOW.name())) {
            Object result = chpService.getInitialChpResults().get(HEATING_WATER_FLOW.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangTemp2(value);
        } else {
            protocol.setMessungAnfangTemp2(null);
        }
        if (chpService.getInitialChpResults().containsKey(ENGINE_COOLANT_RETURN.name())) {
            Object result = chpService.getInitialChpResults().get(ENGINE_COOLANT_RETURN.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangTemp3(value);
        } else {
            protocol.setMessungAnfangTemp3(null);
        }

        if (chpService.getInitialChpResults().containsKey(ENGINE_COOLANT_FLOW.name())) {
            Object result = chpService.getInitialChpResults().get(ENGINE_COOLANT_FLOW.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangTemp4(value);
        } else {
            protocol.setMessungAnfangTemp4(null);
        }

        if (chpService.getInitialChpResults().containsKey(CONTROL_CABINET.name())) {
            Object result = chpService.getInitialChpResults().get(CONTROL_CABINET.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangTemp5(value);
        } else {
            protocol.setMessungAnfangTemp5(null);
        }

        if (chpService.getInitialChpResults().containsKey(HOUSING.name())) {
            Object result = chpService.getInitialChpResults().get(HOUSING.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangTemp6(value);
        } else {
            protocol.setMessungAnfangTemp6(null);
        }
        if (chpService.getInitialChpResults().containsKey(EXHAUST_TEMPERATURE.name())) {
            Object result = chpService.getInitialChpResults().get(EXHAUST_TEMPERATURE.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangTemp7(value);
        } else {
            protocol.setMessungAnfangTemp7(null);
        }
        if (chpService.getInitialChpResults().containsKey(GENERATOR_WINDING.name())) {
            Object result = chpService.getInitialChpResults().get(GENERATOR_WINDING.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangTemp8(value);
        } else {
            protocol.setMessungAnfangTemp8(null);
        }
        if (chpService.getInitialChpResults().containsKey(ENGINE_OIL.name())) {
            Object result = chpService.getInitialChpResults().get(ENGINE_OIL.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangTemp9(value);
        } else {
            protocol.setMessungAnfangTemp9(null);
        }

        if (chpService.getInitialChpResults().containsKey(ENGINE_COOLANT.name())) {
            Object result = chpService.getInitialChpResults().get(ENGINE_COOLANT.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungAnfangTemp10(value);
        } else {
            protocol.setMessungAnfangTemp10(null);
        }

        if (chpService.getLastChpResults().containsKey(OPERATING_HOURS.name())) {
            Object result = chpService.getLastChpResults().get(OPERATING_HOURS.name());
            Double value = null;
            if (result instanceof Long) {
                try {
                    value = ((Long) result).doubleValue();
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeBetriebsstundenInSekunden(value);
        } else {
            protocol.setMessungEndeBetriebsstundenInSekunden(null);
        }


        if (chpService.getLastChpResults().containsKey(START_COUNT.name())) {
            Object result = chpService.getLastChpResults().get(START_COUNT.name());
            Double value = null;
            if (result instanceof Long) {
                try {
                    value = ((Long) result).doubleValue();
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeStarts(value);
        } else {
            protocol.setMessungEndeStarts(null);
        }


        if (energyService.getLastEnergyResults().containsKey(GENERATED_ENERGY.name())) {
            Object result = energyService.getLastEnergyResults().get(GENERATED_ENERGY.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndekWh(value);
        } else {
            protocol.setMessungEndekWh(null);
        }

        if (energyService.getLastEnergyResults().containsKey(ACTIVE_POWER.name())) {
            Object result = energyService.getLastEnergyResults().get(ACTIVE_POWER.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeLeistung(value);
        } else {
            protocol.setMessungEndeLeistung(null);
        }

        if (heatingService.getLastHeatingResults().containsKey(GENERATED_ENERGY_HEATING.name())) {
            Object result = heatingService.getLastHeatingResults().get(GENERATED_ENERGY_HEATING.name());
            Double value = null;
            if (result instanceof Long) {
                try {
                    value = ((Long) result).doubleValue();
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeWaerme(value);
        } else {
            protocol.setMessungEndeWaerme(null);
        }

        if (gasService.getLastGasResults().containsKey(GAS_METER.name())) {
            Double result = gasService.getGasDifference().get();
            if (result != null && !result.isNaN()) {
                protocol.setMessungEndeGas(result);
            } else {
                log.warn("Gas difference is NaN or null — setting MessungEndeGas to null.");
                protocol.setMessungEndeGas(null);
            }
        } else {
            protocol.setMessungEndeGas(null);
        }
        if (gasService.getLastGasResults().containsKey(GAS_TEMPERATURE.name())) {
            Object result = gasService.getLastGasResults().get(GAS_TEMPERATURE.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeGastemperatur(value);
        } else {
            protocol.setMessungEndeGastemperatur(null);
        }
        if (gasService.getLastGasResults().containsKey(GAS_PRESSURE.name())) {
            Object result = gasService.getLastGasResults().get(GAS_PRESSURE.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeGasfliessdruck(value);
        } else {
            protocol.setMessungEndeGasfliessdruck(null);
        }
        if (gasService.getLastGasResults().containsKey(AMBIENT_PRESSURE.name())) {
            Object result = gasService.getLastGasResults().get(AMBIENT_PRESSURE.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeLuftdruck(value);
        } else {
            protocol.setMessungEndeLuftdruck(null);
        }
        if (gasService.getLastGasResults().containsKey(AMBIENT_TEMPERATURE.name())) {
            Object result = gasService.getLastGasResults().get(AMBIENT_TEMPERATURE.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeLufttemperatur(value);
        } else {
            protocol.setMessungEndeLufttemperatur(null);
        }
        if (heatingService.getLastHeatingResults().containsKey(RETURN_TEMPERATURE.name())) {
            Object result = heatingService.getLastHeatingResults().get(RETURN_TEMPERATURE.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeTemp1WMZ(value);
        } else {
            protocol.setMessungEndeTemp1WMZ(null);
        }

        if (heatingService.getLastHeatingResults().containsKey(SUPPLY_TEMPERATURE.name())) {
            Object result = heatingService.getLastHeatingResults().get(SUPPLY_TEMPERATURE.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeTemp2WMZ(value);
        } else {
            protocol.setMessungEndeTemp2WMZ(null);
        }

        if (heatingService.getLastHeatingResults().containsKey(VOLUME_FLOW.name())) {
            Object result = heatingService.getLastHeatingResults().get(VOLUME_FLOW.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeVolumenstromWMZ(value);
        } else {
            protocol.setMessungEndeVolumenstromWMZ(null);
        }

        if (chpService.getLastChpResults().containsKey(HEATING_WATER_RETURN.name())) {
            Object result = chpService.getLastChpResults().get(HEATING_WATER_RETURN.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeTemp1(value);
        } else {
            protocol.setMessungEndeTemp1(null);
        }

        if (chpService.getLastChpResults().containsKey(HEATING_WATER_FLOW.name())) {
            Object result = chpService.getLastChpResults().get(HEATING_WATER_FLOW.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeTemp2(value);
        } else {
            protocol.setMessungEndeTemp2(null);
        }

        if (chpService.getLastChpResults().containsKey(ENGINE_COOLANT_RETURN.name())) {
            Object result = chpService.getLastChpResults().get(ENGINE_COOLANT_RETURN.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeTemp3(value);
        } else {
            protocol.setMessungEndeTemp3(null);
        }

        if (chpService.getLastChpResults().containsKey(ENGINE_COOLANT_FLOW.name())) {
            Object result = chpService.getLastChpResults().get(ENGINE_COOLANT_FLOW.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeTemp4(value);
        } else {
            protocol.setMessungEndeTemp4(null);
        }

        if (chpService.getLastChpResults().containsKey(CONTROL_CABINET.name())) {
            Object result = chpService.getLastChpResults().get(CONTROL_CABINET.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeTemp5(value);
        } else {
            protocol.setMessungEndeTemp5(null);
        }
        if (chpService.getLastChpResults().containsKey(HOUSING.name())) {
            Object result = chpService.getLastChpResults().get(HOUSING.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeTemp6(value);
        } else {
            protocol.setMessungEndeTemp6(null);
        }

        if (chpService.getLastChpResults().containsKey(EXHAUST_TEMPERATURE.name())) {
            Object result = chpService.getLastChpResults().get(EXHAUST_TEMPERATURE.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeTemp7(value);
        } else {
            protocol.setMessungEndeTemp7(null);
        }

        if (chpService.getLastChpResults().containsKey(GENERATOR_WINDING.name())) {
            Object result = chpService.getLastChpResults().get(GENERATOR_WINDING.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeTemp8(value);
        } else {
            protocol.setMessungEndeTemp8(null);
        }

        if (chpService.getLastChpResults().containsKey(ENGINE_OIL.name())) {
            Object result = chpService.getLastChpResults().get(ENGINE_OIL.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeTemp9(value);
        } else {
            protocol.setMessungEndeTemp9(null);
        }

        if (chpService.getLastChpResults().containsKey(ENGINE_COOLANT.name())) {
            Object result = chpService.getLastChpResults().get(ENGINE_COOLANT.name());
            Double value = null;
            if (result instanceof String) {
                try {
                    value = Double.parseDouble((String) result);
                } catch (NumberFormatException e) {
                    // handle error
                }
            }
            protocol.setMessungEndeTemp10(value);
        } else {
            protocol.setMessungEndeTemp10(null);
        }


        return productionProtocolRepository.save(protocol);
    }
}
