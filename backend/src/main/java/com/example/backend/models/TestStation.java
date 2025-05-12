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
public class TestStation {
  int id;
  String testStationName;
  private List<ModbusDevice> modbusdevices;
}