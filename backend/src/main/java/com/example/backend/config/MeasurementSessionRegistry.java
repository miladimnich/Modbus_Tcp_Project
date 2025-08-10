package com.example.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;


@Getter
@Setter
@Component
public class MeasurementSessionRegistry {

    private volatile Integer serialNumber;

    public void registerSerialNumber(int serialNumber) {
        this.serialNumber = serialNumber;
    }
}
