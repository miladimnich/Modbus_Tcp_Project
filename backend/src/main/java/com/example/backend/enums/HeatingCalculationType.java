package com.example.backend.enums;

import com.example.backend.models.SubDevice;

public enum HeatingCalculationType {
  ERZEUGTE_ENERGIE_HEATING(10),
  VORLAUFTEMPERATUR(20),
  VOLUMENSTROM(30),
  TEMPERATURDIFFERENZ(40),
  LEISTUNG(50),
  GESAMT_VOLUMEN(60),
  RuCKLAUFTEMPERATUR(70);



  private final int offset;

  HeatingCalculationType(int offset) {
    this.offset = offset;
  }

  public int getStartAddress(SubDevice subDevice) {
    int baseAddress = subDevice.getStartAddress();
    return baseAddress + offset;
  }
}
