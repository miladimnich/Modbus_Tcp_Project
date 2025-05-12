package com.example.backend.enums;

import com.example.backend.models.SubDevice;

public enum BhkwCalculationType {
  EXHAUST_GAS_TEMPERATURE(40018),
  HEATING_WATER_FLOW(40025),
  HEATING_WATER_RETURN(40026),
  ENGINE_COOLANT_RETURN(40027),
  ENGINE_COOLANT_FLOW(40028),
  CONTROLLER(40029),
  BOX(40030),
  GENERATOR_WINDING(40031),
  ENGINE_OIL(40032);

  private final int offset;

  BhkwCalculationType(int offset) {
    this.offset = offset;
  }

  public int getStartAddress(SubDevice subDevice) {
    int baseAddress = subDevice.getStartAddress();
    return baseAddress + offset;
  }


}