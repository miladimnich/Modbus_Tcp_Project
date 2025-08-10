package com.example.backend.enums;


import com.example.backend.models.SubDevice;

public enum ChpCalculationType { // Combined Heat and Power BhkwTypes
    EXHAUST_TEMPERATURE(40018),
    HEATING_WATER_RETURN(40026),
    HEATING_WATER_FLOW(40025),
    ENGINE_COOLANT_RETURN(40027),
    ENGINE_COOLANT_FLOW(40028),
    ENGINE_COOLANT(40033),
    CONTROL_CABINET(40029),
    HOUSING(40030),
    GENERATOR_WINDING(40031),
    ENGINE_OIL(40032),
    OPERATING_HOURS(43587), //2
    START_COUNT(43589);

    private final int offset;

    ChpCalculationType(int offset) {
        this.offset = offset;
    }

    public int getStartAddress(SubDevice subDevice) {
        int baseAddress = subDevice.getStartAddress();
        return baseAddress + offset;
    }
}
