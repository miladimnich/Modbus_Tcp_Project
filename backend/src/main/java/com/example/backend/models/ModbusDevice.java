package com.example.backend.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ModbusDevice {
    String ipAddress;
    private int port;
    private List<SubDevice> subDevices;
}
