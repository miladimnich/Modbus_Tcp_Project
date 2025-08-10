package com.example.backend.service.efficiency;

import com.example.backend.config.socket.WebSocketHandlerCustom;
import com.example.backend.enums.GasDataType;
import com.example.backend.events.GasCalculationCompleteEvent;
import com.example.backend.service.energy.EnergyService;
import com.example.backend.service.gas.GasService;
import com.example.backend.service.heating.HeatingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings("unused")
public class EfficiencyCalculationService {
    private final WebSocketHandlerCustom webSocketHandlerCustom;
    private final GasService gasService;
    private final EnergyService energyService;
    private final HeatingService heatingService;

    @Autowired
    public EfficiencyCalculationService(WebSocketHandlerCustom webSocketHandlerCustom, GasService gasService, EnergyService energyService, HeatingService heatingService) {
        this.webSocketHandlerCustom = webSocketHandlerCustom;
        this.gasService = gasService;
        this.energyService = energyService;
        this.heatingService = heatingService;
    }

    @EventListener
    @Async
    public void handleGasCalculationCompleteEvent(GasCalculationCompleteEvent event) {
        int testStationId = event.getTestStationId();

        try {
            calculateElectricalEfficiency(testStationId);
        } catch (Exception e) {
            log.error("Failed to calculate electrical efficiency for testStationId {}: {}", testStationId, e.getMessage(), e);
        }

        try {
            calculateThermalEfficiency(testStationId);
        } catch (Exception e) {
            log.error("Failed to calculate thermal efficiency for device {}: {}", testStationId, e.getMessage(), e);
        }

        try {
            calculateOverallEfficiency(testStationId);
        } catch (Exception e) {
            log.error("Failed to calculate overall efficiency for device {}: {}", testStationId, e.getMessage(), e);
        }
    }


    public void calculateElectricalEfficiency(int testStationId) {
        Double electricalPower = energyService.getEnergyDifference().get();
        Double gasPower = gasService.getGasPowerResult().get();
        if (electricalPower.isNaN() || gasPower.isNaN()) {
            log.warn("Electrical power or gas power is NaN for testStationId: {}", testStationId);
            return;
        }
        try {
            double result = (electricalPower / gasPower) * 100;
            log.info("Calculated electrical efficiency: {}% for testStationId: {}", result, testStationId);
            sendEfficiencyUpdateToClient(testStationId, GasDataType.ELECTRICAL_EFFICIENCY, result);
        } catch (ArithmeticException e) {
            log.error("ArithmeticException while calculating electrical efficiency for testStationId {}: {}", testStationId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected exception while calculating electrical efficiency for testStationId {}: {}", testStationId, e.getMessage(), e);
        }
    }


    public void calculateThermalEfficiency(int testStationId) {
        if (heatingService.hasHeatingSubDevice(testStationId)) {
            log.info("Skipping thermal efficiency calculation: no HEATING subdevice found for testStationId {}", testStationId);
            return;
        }
        try {
            Double thermalPower = heatingService.getHeatingDifference().get();
            Double gasPower = gasService.getGasPowerResult().get();

            if (gasPower.isNaN() || thermalPower.isNaN()) {
                log.warn("Thermal power or gas power is NaN for testStationId {}", testStationId);
                return;
            }
            double result = thermalPower / gasPower * 100;
            log.info("Calculated thermal efficiency: {}% for testStationId {}", result, testStationId);
            sendEfficiencyUpdateToClient(testStationId, GasDataType.THERMAL_EFFICIENCY, result);
        } catch (ArithmeticException e) {
            log.error("ArithmeticException while calculating thermal efficiency for testStationId {}: {}", testStationId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error calculating thermal efficiency for testStationId {}: {}", testStationId, e.getMessage(), e);
        }

    }


    public void calculateOverallEfficiency(int testStationId) {
        try {
            Double electricalPower = energyService.getEnergyDifference().get();
            Double gasPower = gasService.getGasPowerResult().get();
            Double thermalPower = heatingService.getHeatingDifference().get();

            if (electricalPower.isNaN() || gasPower.isNaN() || thermalPower.isNaN()) {
                log.warn("One or more input values are NaN (electrical={}, gas={}, thermal={}) for testStationId {}",
                        electricalPower, gasPower, thermalPower, testStationId);
                return;
            }
            double result = ((electricalPower + thermalPower) / gasPower) * 100;
            log.info("Calculated overall efficiency: {}% for testStationId {}", result, testStationId);
            sendEfficiencyUpdateToClient(testStationId, GasDataType.OVERALL_EFFICIENCY, result);
        } catch (ArithmeticException e) {
            log.error("ArithmeticException while calculating overall efficiency for testStationId {}: {}", testStationId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error calculating overall efficiency for testStationId {}: {}", testStationId, e.getMessage(), e);
        }
    }

    private void sendEfficiencyUpdateToClient(int testStationId, GasDataType type, double value) {
        String formattedResult = String.format(Locale.US, "%.2f", value);
        Map<String, Object> update = new LinkedHashMap<>();
        update.put(type.name(), formattedResult);
        update.put("testStationId", testStationId);

        log.debug("Pushing update {} for testStationId {}", update, testStationId);
        if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
            webSocketHandlerCustom.enqueueUpdate(update);
        }
    }
}
