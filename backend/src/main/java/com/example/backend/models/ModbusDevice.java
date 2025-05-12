package com.example.backend.models;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ModbusDevice {
  String ipAddress;
  private int port;
  private List<SubDevice> subDevices;
}