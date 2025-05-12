package com.example.backend.enums;

import com.example.backend.models.SubDevice;


public enum BhkwCalculationType {
  ABGAS_TEMPERATUR(40018),
  HEIZUNGS_WASSER_VORLAUF(40025),
  HEIZUNGS_WASSER_RUCKLAUF(40026),
  MOTOR_KUHLMITTEL_RUCKLAUF(40027),
  MOTOR_KUHLMITTEL_VORLAUF(40028),
  SCHALTSCHRANK(40029),
  GEHAUSE(40030),
  GENERATOR_WICKLUNG(40031),
  MOTOR_OIL(40032),
  BETRIEBS_STUNDEN(43587),
  START_ANZAHL(43589);

  private final int offset;

  BhkwCalculationType(int offset) {
    this.offset = offset;
  }

  public int getStartAddress(SubDevice subDevice) {
    int baseAddress = subDevice.getStartAddress();
    return baseAddress + offset;
  }


}