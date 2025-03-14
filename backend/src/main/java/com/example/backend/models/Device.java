package com.example.backend.models;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Device {
  private int id;
  private String testStationName;
  private String ipAddress;
  private int port;
  private List<SubDevice> subDevices;
}
