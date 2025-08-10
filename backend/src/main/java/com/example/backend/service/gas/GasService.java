package com.example.backend.service.gas;

import com.example.backend.config.MeasurementSessionRegistry;
import com.example.backend.config.socket.WebSocketHandlerCustom;
import com.example.backend.config.threading.ScheduledTaskRegistry;
import com.example.backend.enums.GasCalculationType;
import com.example.backend.enums.GasDataType;
import com.example.backend.enums.SubDeviceType;
import com.example.backend.events.GasCalculationCompleteEvent;
import com.example.backend.events.StopPollingEvent;
import com.example.backend.exception.GasProcessingException;
import com.example.backend.exception.ModbusDeviceException;
import com.example.backend.models.*;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductionProtocolRepository;
import com.example.backend.service.modbus.ModbusBitwiseService;
import com.example.backend.service.teststation.TestStationService;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.sero.messaging.TimeoutException;
import com.serotonin.modbus4j.sero.messaging.WaitingRoomException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.backend.enums.GasCalculationType.GAS_METER;


@Slf4j
@Service
@Getter
@Setter
public class GasService {
    private final Map<String, Object> initialGasResults = new ConcurrentHashMap<>();
    private final Map<String, Object> currentGasResults = new ConcurrentHashMap<>();
    private final Map<String, Object> lastGasResults = new ConcurrentHashMap<>();
    private final Map<String, Object> previousGasResults = new ConcurrentHashMap<>();
    private final Map<Integer, Double> hoDailyValueCache = new ConcurrentHashMap<>();

    private final AtomicReference<Double> gasDifference = new AtomicReference<>(Double.NaN);
    private final AtomicReference<Double> gasAmbientPressure = new AtomicReference<>(Double.NaN);
    private final AtomicReference<Double> gasPressure = new AtomicReference<>(Double.NaN);
    private final AtomicReference<Double> gasTemperature = new AtomicReference<>(Double.NaN);
    private final AtomicReference<Double> gasPowerResult = new AtomicReference<>(Double.NaN);

    private final AtomicBoolean isInitialResultsPopulated = new AtomicBoolean(false);

    private final AtomicBoolean shouldStop = new AtomicBoolean(false);
    private long autoStopDurationMinutes = 120;
    private volatile long timestampNow = -1L;


    private final ScheduledExecutorService executorService;
    private final ScheduledTaskRegistry scheduledTaskRegistry;

    private final ApplicationEventPublisher eventPublisher;
    private final ModbusBitwiseService modbusBitwiseService;
    private final TestStationService testStationService;
    private final WebSocketHandlerCustom webSocketHandlerCustom;
    private final MeasurementSessionRegistry measurementSessionRegistry;
    private final ProductRepository productRepository;
    private final ProductionProtocolRepository productionProtocolRepository;


    @Autowired
    public GasService(ScheduledExecutorService executorService, ScheduledTaskRegistry scheduledTaskRegistry, ApplicationEventPublisher eventPublisher, ModbusBitwiseService modbusBitwiseService,
                      TestStationService testStationService, WebSocketHandlerCustom webSocketHandlerCustom, MeasurementSessionRegistry measurementSessionRegistry, ProductRepository productRepository, ProductionProtocolRepository productionProtocolRepository) {
        this.executorService = executorService;
        this.scheduledTaskRegistry = scheduledTaskRegistry;
        this.eventPublisher = eventPublisher;
        this.modbusBitwiseService = modbusBitwiseService;
        this.testStationService = testStationService;
        this.webSocketHandlerCustom = webSocketHandlerCustom;
        this.measurementSessionRegistry = measurementSessionRegistry;
        this.productRepository = productRepository;
        this.productionProtocolRepository = productionProtocolRepository;
        // this.pollingControlService = pollingControlService;
    }

    public void processGasData(int testStationId) throws InterruptedException {
        log.info("Starting gas data processing for TestStation ID: {}", testStationId);
        TestStation testStation = testStationService.getTestStationById(testStationId);
        if (testStation == null) {
            log.warn("No TestStation found for ID: {}", testStationId);
            return;
        }
        for (ModbusDevice modbusDevice : testStation.getModbusDevices()) {
            for (SubDevice subDevice : modbusDevice.getSubDevices()) {
                if (!subDevice.getType().equals(SubDeviceType.GAS)) {
                    log.debug("Skipping SubDevice with slaveId={}, startAddress={}, type={}",
                            subDevice.getSlaveId(), subDevice.getStartAddress(), subDevice.getType());
                    continue;
                }
                List<GasCalculationType> gasCalculationTypes = subDevice.getGasCalculationTypes();
                if (gasCalculationTypes == null) continue;
                for (GasCalculationType gasCalculationType : gasCalculationTypes) {
                    int startAddress = gasCalculationType.getStartAddress(subDevice);
                    try {
                        log.debug("Calculating {} for SubDevice Type: {} at address {}", gasCalculationType.name(), subDevice.getType(), startAddress);
                        long result = modbusBitwiseService.bitwiseShiftCalculation(
                                testStationId, startAddress, modbusDevice, subDevice);
                        log.info("result for start address {} is {}", startAddress, result);
                        switch (gasCalculationType) {
                            case GAS_TEMPERATURE -> {
                                gasTemperature.set((double) result / 100.0);
                                processAndPushCurrentResults(testStationId, gasCalculationType.name(), gasTemperature.get());
                            }
                            case GAS_METER -> processAndPushCurrentResults(testStationId, gasCalculationType.name(), result);
                            case AMBIENT_TEMPERATURE ->
                                    processAndPushCurrentResults(testStationId, gasCalculationType.name(), (double) result / 100);
                            case AMBIENT_PRESSURE -> {
                                gasAmbientPressure.set((double) (result) * 0.074064361 - 1.176470588);
                                processAndPushCurrentResults(testStationId, gasCalculationType.name(), gasAmbientPressure.get());
                            }
                            case GAS_PRESSURE -> {
                                double converted = switch (startAddress) {
                                    case 33 -> (double) result * 0.07398380 + 2.869148;
                                    case 35 -> (double) result * 0.0740486 + 0.0327669;
                                    case 37 -> (double) result * 0.074172 + 4.156304;
                                    default -> {
                                        log.warn("Unknown startAddress for GAS_PRESSURE: {} in SubDevice Type: {}", startAddress, subDevice.getType());
                                        yield (double) result; // Still yield a value to avoid breaking the switch
                                    }
                                };
                                gasPressure.set(converted);
                                if (gasAmbientPressure.get().isNaN() || gasPressure.get().isNaN()) {
                                    log.debug("Ambient or gas pressure is unavailable while calculating {} for SubDevice type {} at address {}", gasCalculationType.name(), subDevice.getType(), startAddress);
                                    continue;
                                }
                                double diff = gasPressure.get() - gasAmbientPressure.get();
                                processAndPushCurrentResults(testStationId, gasCalculationType.name(), diff);
                            }
                            default -> log.warn("Unhandled EnergyCalculationType: {}", gasCalculationType);
                        }
                    } catch (InterruptedException ie) {
                        log.warn("Thread interrupted during gas data processing for device {}", testStationId, ie);
                        Thread.currentThread().interrupt(); // preserve interrupt status
                        throw ie; // propagate InterruptedException
                    } catch (ModbusDeviceException | WaitingRoomException | TimeoutException |
                             IllegalStateException | ModbusTransportException ex) {
                        log.error("Critical exception [{} - {}]: {}", gasCalculationType.name(), testStationId, ex.getMessage(), ex);
                        throw new GasProcessingException("Critical failure in gas processing", ex);
                    } catch (Exception ex) {
                        log.error("Unexpected exception [{} - {}]: {}", gasCalculationType.name(), testStationId, ex.getMessage(), ex);
                        Thread.currentThread().interrupt();
                        throw new InterruptedException("Thread interrupted due to exception: " + ex.getMessage());
                    }
                }
            }
        }
    }


    private void processAndPushCurrentResults(int testStationId, String key, Object value) {
        if (value == null) {
            log.warn("Null value received for key '{}', skipping update for testStationId {}", key, testStationId);
            return;
        }
        Object processedValue = value;
        // Automatically format doubles to string with two decimals
        if (value instanceof Double doubleVal) {
            if (Double.isNaN(doubleVal)) {
                log.warn("NaN value detected for key '{}', skipping update for testStationId {}", key, testStationId);
                return;
            }
            processedValue = String.format(Locale.US, "%.2f", doubleVal);
        }

        log.debug("Processing value: Key = {}, Value = {}, testStationId = {}", key, processedValue, testStationId);
        try {
            Object lastValue = lastGasResults.get(key);
            log.debug("🔎 Previous value for key '{}': {}", key, lastValue);

            boolean valueChanged = (lastValue == null || !lastValue.equals(processedValue)); // true if the value is null or different
            log.debug("🗂 Before update - currentChpResults[{}]: {}", key, currentGasResults.get(key));
            currentGasResults.put(key, processedValue);
            log.debug("📌 After update - currentChpResults[{}]: {}", key, currentGasResults.get(key));

            if (valueChanged) {
                lastGasResults.put(key, processedValue);

                Map<String, Object> update = new LinkedHashMap<>();
                update.put(key, processedValue);
                update.put("testStationId", testStationId);

                if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
                    webSocketHandlerCustom.enqueueUpdate(update);
                }

            } else {
                log.debug("Value did not change, skipping push for key: {}, testStationId: {}", key, testStationId);
            }
        } catch (Exception e) {
            log.error("Unexpected error in energy  processAndPush for key: {}, testStationId: {} - {}", key, testStationId, e.getMessage(), e);
        }
    }


    public void calculateAndPushMeterDifference(int testStationId) throws InterruptedException {
        log.info("Calculating meter difference for testStationId: {}", testStationId);
        try {
            List<SubDevice> gasSubDevices = testStationService.getSubDevicesByType(testStationId, //adr 1
                    SubDeviceType.GAS);

            if (gasSubDevices == null || gasSubDevices.isEmpty()) {
                log.warn("No GAS SubDevices found for testStationId: {}", testStationId);
                return;
            }

            for (SubDevice subDevice : gasSubDevices) {
                List<GasCalculationType> types = subDevice.getGasCalculationTypes();

                if (types == null || types.isEmpty()) {
                    log.debug("No GasCalculationTypes for SubDevice [slaveId={}, startAddress={}, type={}]",
                            subDevice.getSlaveId(), subDevice.getStartAddress(), subDevice.getType());
                    continue;
                }

                for (GasCalculationType type : types) {
                    String key = type.name();
                    if (type != GAS_METER) continue;

                    Object current = currentGasResults.get(key);
                    if (current == null) {
                        log.warn("Current gas value is null for key: {}. Skipping calculation.", key);
                        continue;
                    }
                    long currentVal;
                    try {
                        currentVal = parseLong(current);
                    } catch (NumberFormatException nfe) {
                        log.error("Failed to parse current gas value for key {}: {}", key, nfe.getMessage(), nfe);
                        continue;
                    }
                    Object previous = previousGasResults.get(key);
                    if (previous == null) {
                        log.debug("Previous value not found for key: {}. Initializing with current value: {}", key, currentVal);
                        previousGasResults.put(key, currentVal);
                        continue; // Don't stop the whole method — just skip this iteration
                    }
                    long previousVal;
                    try {
                        previousVal = parseLong(previous);
                    } catch (NumberFormatException nfe) {
                        log.error("Failed to parse previous gas value for key {}: {}", key, nfe.getMessage(), nfe);
                        continue;
                    }
                    try {
                        double count = (previousVal != currentVal) ? 0.1 : 0.0; // if true 0.1 else 0.0
                        if (count == 0) {
                            log.debug("No change in {} value ({} == {}). Skipping.", key, previousVal, currentVal);
                            return;
                        }
                        if (!isInitialResultsPopulated.get()) {
                            if (initialGasResults.isEmpty()) {
                                timestampNow = System.currentTimeMillis();
                                initialGasResults.putAll(currentGasResults);
                                previousGasResults.put(key, currentVal);
                                isInitialResultsPopulated.set(true);

                                ScheduledFuture<?> stopTask = executorService.schedule(() -> {
                                    try {
                                        if (Thread.currentThread().isInterrupted()) {
                                            throw new InterruptedException();
                                        }
                                        shouldStop.set(true);
                                        log.info("Stop task executed after {} hours.", autoStopDurationMinutes);
                                    } catch (Exception e) {
                                        log.error("Unexpected exception in stop task: {}", e.getMessage(), e);
                                    } catch (Throwable t) {
                                        log.error("Fatal error in stop task thread.", t);
                                    }
                                }, autoStopDurationMinutes, TimeUnit.MINUTES);
                                scheduledTaskRegistry.register(testStationId, stopTask);
                                log.debug("Initial results populated and stop task scheduled.");
                                return;
                            }
                        }

                        if (Double.isNaN(gasDifference.get())) {
                            log.debug("Gas difference is NaN. Initializing to 0.0.");
                            gasDifference.set(0.0);
                        }

                        //     gasDifference.updateAndGet(val -> val + count);


                        gasDifference.updateAndGet(oldVal -> {
                            double newVal = (oldVal == null || Double.isNaN(oldVal)) ? 0.1 : oldVal + 0.1;
                            return Math.round(newVal * 10) / 10.0;  // round to 1 decimal place
                        });


                        log.debug("Updated gasDifference for key {}: new value = {}", key, gasDifference.get());

                        Map<String, Object> update = new LinkedHashMap<>();
                        update.put(key, String.format(Locale.US, "%.2f", gasDifference.get()));
                        update.put("testStationId", testStationId);
                        update.put("difference", key);

                        if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
                            webSocketHandlerCustom.enqueueUpdate(update);
                        }

                        calculateGasPower(testStationId, gasAmbientPressure.get(), gasPressure.get(), gasTemperature.get(), gasDifference.get());

                        if (shouldStop.get()) {
                            log.info("Auto-stop triggered. Publishing StopPollingEvent for testStationId: {}", testStationId);
                            eventPublisher.publishEvent(new StopPollingEvent(this, testStationId));
                            return;
                        }
                        previousGasResults.put(key, currentVal);
                        log.debug("Updated previousGasResults for key {} with value {}", key, currentVal);
                    } catch (NumberFormatException nfe) {
                        log.error("Failed to parse gas meter value for key {}: {}", key, nfe.getMessage(), nfe);
                    } catch (Exception e) {
                        log.error("Unexpected error processing key {}: {}", key, e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to calculate meter difference for testStationId {}: {}", testStationId, e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new InterruptedException("Thread interrupted due to exception: " + e.getMessage());
        }
    }


    private void calculateGasPower(int testStationId, Double environmentPressure, Double gasPressure,
                                   Double gasTemperature, Double gasMeter) {

        try {
            if (environmentPressure == null || gasPressure == null || gasTemperature == null || gasMeter == null) {
                log.warn("Null input values detected for testStationId {}: envPressure={}, gasPressure={}, gasTemp={}, gasMeter={}",
                        testStationId, environmentPressure, gasPressure, gasTemperature, gasMeter);
                return;
            }
            if (environmentPressure.isNaN() || gasPressure.isNaN() || gasTemperature.isNaN() || gasMeter.isNaN()) {
                log.warn("Skipping gas power calculation due to invalid (NaN) input values for testStationId: {}", testStationId);
                return;
            }

            Double ho = hoDailyValueCache.computeIfAbsent(testStationId, this::fetchHoDailyValueFromDB);
            System.out.println("ho from database " + ho);
            if (ho == null || ho.isNaN()) {
                log.warn("Invalid or missing dailyValue (ho) value for testStationId {}", testStationId);
                return;
            }

            double result = (ho * 0.9017) * ((gasPressure) / 1013.15)
                    * (273.0 / (273.0 + gasTemperature)) * gasMeter;
            log.info("Calculated gas power (GasPower) for testStationId {}: {}", testStationId, result);
            gasPowerResult.set(result);

            String formattedResult = String.format(Locale.US, "%.2f", gasPowerResult.get());
            Map<String, Object> update = new LinkedHashMap<>();
            update.put(GasDataType.GAS_POWER.name(), formattedResult);
            update.put("testStationId", testStationId);


            if (!webSocketHandlerCustom.getConnectedSessions().isEmpty()) {
                webSocketHandlerCustom.enqueueUpdate(update);
            }
            eventPublisher.publishEvent(new GasCalculationCompleteEvent(this, testStationId));
            log.debug("Published GasCalculationCompleteEvent for testStationId {}", testStationId);

        } catch (Exception e) {
            log.error("Unexpected error during gas power calculation for testStationId {}: {}", testStationId, e.getMessage(), e);
        }
    }


    private Double fetchHoDailyValueFromDB(int testStationId) {
        try {
            Integer serialNumber = measurementSessionRegistry.getSerialNumber();
            if (serialNumber == null) {
                log.warn("No serial number found for testStationId: {}", testStationId);
                return Double.NaN;
            }

            ProductStatus product = productRepository.findBySerialNumber(serialNumber);
            if (product == null) {
                log.warn("No ProductStatus found for serialNumber: {}", serialNumber);
                return Double.NaN;
            }

            Integer objectNumber = product.getObjectNumber();
            if (objectNumber == null) {
                log.warn("ProductStatus for serialNumber {} has null objectNumber", serialNumber);
                return Double.NaN;
            }

            ProductionProtocol protocol = productionProtocolRepository.findByDirectReference1Number(objectNumber);
            if (protocol == null) {
                log.warn("No ProductionProtocol found for objectNumber: {}", objectNumber);
                return Double.NaN;
            }

            Double hoDailyValue = protocol.getDailyMeasurementValue();
            if (hoDailyValue == null) {
                log.warn("Daily measurement value is null in production protocol for object number: {}", objectNumber);
                return Double.NaN;
            }
            log.debug("Daily measurement value: {} successfully retrieved for device ID: {}", hoDailyValue, testStationId);
            return hoDailyValue;
        } catch (Exception e) {
            log.error("Error fetching daily measurement value from DB for device ID {}: {}", testStationId, e.getMessage(), e);
            return Double.NaN;
        }
    }

    private long parseLong(Object value) throws NumberFormatException {
        return (value instanceof Number) ? ((Number) value).longValue() : Long.parseLong(value.toString());
    }

    public synchronized Map<String, Object> getStartGasResults(int testStationId) {
        log.debug("Saving current gas results as start results for testStationId: {}", testStationId);
        return new HashMap<>(initialGasResults);
    }

    public Map<String, Object> getLastGasResults(int testStationId) {
        log.debug("Fetching last gas results for testStationId: {}", testStationId);
        return new HashMap<>(lastGasResults);
    }

    public void setAutoStopDurationMinutes(long minutes) {
        this.autoStopDurationMinutes = minutes;
        log.info("Auto-stop duration updated to {} minutes", minutes);
    }


    public synchronized void clearGasResults() {
        initialGasResults.clear();
        currentGasResults.clear();
        lastGasResults.clear();
        previousGasResults.clear();
        gasDifference.set(Double.NaN);
        gasAmbientPressure.set(Double.NaN);
        gasPressure.set(Double.NaN);
        gasTemperature.set(Double.NaN);
        isInitialResultsPopulated.set(false);
        shouldStop.set(false);
        gasPowerResult.set(Double.NaN);
        hoDailyValueCache.clear();
    }
}




























