package com.example.backend.enums;


import com.example.backend.models.SubDevice;

public enum HeatingCalculationType {
    GENERATED_ENERGY_HEATING(10),
    SUPPLY_TEMPERATURE(50),
    VOLUME_FLOW(40),
    TEMPERATURE_DIFFERENCE(70),
    POWER(30),
    TOTAL_VOLUME(20),
    RETURN_TEMPERATURE(60);



    private final int offset;

    HeatingCalculationType(int offset) {
        this.offset = offset;
    }

    public int getStartAddress(SubDevice subDevice) {
        int baseAddress = subDevice.getStartAddress();
        return baseAddress + offset;
    }


}
