package com.example.backend.models;

import com.example.backend.enums.SubDeviceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubDevice {

  private int startAddress;
  private int slaveId;
  private int registersQuantity;
  private SubDeviceType type;

}
