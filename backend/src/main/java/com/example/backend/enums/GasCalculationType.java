package com.example.backend.enums;

import com.example.backend.models.SubDevice;


public enum GasCalculationType {
  GAS_TEMPERATUR(0),
  GAS_ZAHLER(24),
  GAS_DRUCK(32),
  UMGEBUNGS_TEMPERATURE(-1),  // Special case, will be handled separately
  UMGEBUNGS_DRUCK(-1);



  private final int offset;

  GasCalculationType(int offset) {
    this.offset = offset;
  }

  public int getStartAddress(SubDevice subDevice) {
    int baseAddress = subDevice.getStartAddress();
    if (this == UMGEBUNGS_TEMPERATURE) {
      return 7; // Fixed start address for ENVIRONMENT_TEMPERATURE
    } else if (this == UMGEBUNGS_DRUCK) {
      return 39;
    } else {
      return baseAddress + offset;
    }
  }
}