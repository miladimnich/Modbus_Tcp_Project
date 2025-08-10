package com.example.backend.enums;

 import com.example.backend.models.SubDevice;
 import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum EnergyCalculationType {
    GENERATED_ENERGY(10),
    CONSUMED_ENERGY(20),
    ACTIVE_POWER(30),
    REACTIVE_POWER_BLIND_POWER(40),
    APPARENT_POWER_RESERVED(50),
    VOLTAGE_L1_VOLTS(60),
    VOLTAGE_L2_VOLTS(70),
    VOLTAGE_L3_VOLTS(80),
    FREQUENCY(90),
    CURRENT(100),
    COS_PHI;

    private final int offset;

    EnergyCalculationType(int offset) {
        this.offset = offset;
    }

    // No-arg constructor
    EnergyCalculationType() {
        this.offset = 0; // or some default
    }

    public int getStartAddress(SubDevice subDevice) {
        int baseAddress = subDevice.getStartAddress();
        if (this == COS_PHI) {
            log.warn("No address for COS_PHI");
            return -1;
        }
        return baseAddress + offset;
    }
}
