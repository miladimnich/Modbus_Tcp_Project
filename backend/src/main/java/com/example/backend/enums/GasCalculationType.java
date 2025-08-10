package com.example.backend.enums;


import com.example.backend.models.SubDevice;

public enum GasCalculationType {
    GAS_TEMPERATURE(0),
    GAS_METER(24),
    AMBIENT_TEMPERATURE(-1),  // Special case, will be handled separately
    AMBIENT_PRESSURE(-1),
    GAS_PRESSURE(32);





    private final int offset;

    GasCalculationType(int offset) {
        this.offset = offset;
    }

    public int getStartAddress(SubDevice subDevice) {
        int baseAddress = subDevice.getStartAddress();
        if (this == AMBIENT_TEMPERATURE) {
            return 7; // Fixed start address for ENVIRONMENT_TEMPERATURE
        } else if (this == AMBIENT_PRESSURE) {
            return 39;
        } else {
            return baseAddress + offset;
        }
    }
}
