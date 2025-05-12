package com.example.backend.enums;

import com.example.backend.models.SubDevice;

public enum HeatingCalculationType {
  ERZEUGTE_ENERGIE_HEATING(10),
  VORLAUFTEMPERATUR(50),
  VOLUMENSTROM(40),
  TEMPERATURDIFFERENZ(70),
  LEISTUNG(30),
  GESAMT_VOLUMEN(20),
  RuCKLAUFTEMPERATUR(60);



  private final int offset;

  HeatingCalculationType(int offset) {
    this.offset = offset;
  }

  public int getStartAddress(SubDevice subDevice) {
    int baseAddress = subDevice.getStartAddress();
    return baseAddress + offset;
  }


}