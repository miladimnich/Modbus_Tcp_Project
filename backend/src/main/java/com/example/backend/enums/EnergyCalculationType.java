package com.example.backend.enums;

import com.example.backend.models.SubDevice;

public enum EnergyCalculationType {
  ERZEUGTE_ENERGIE(10),
  GENUTZTE_ENERGIE(20),
  WIRKLEISTUNG(30),
  BLINDLEISTUNG_REACTIVPOWER(40),
  SCHEINLEISTUNG_RESERVED(50),
  SPANNUNG_L1_VOLTS(60),
  SPANNUNG_L2_VOLTS(70),
  SPANNUNG_L3_VOLTS(80),
  FREQUENZ(90),
  STROM(100);



  private final int offset;

  EnergyCalculationType(int offset) {
    this.offset = offset;
  }

  public int getStartAddress(SubDevice subDevice) {
    int baseAddress = subDevice.getStartAddress();
    return baseAddress + offset;
  }
}