package com.example.backend.models;

 import com.example.backend.enums.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubDevice {

    private int startAddress;
    private int slaveId;
    private int registersQuantity;
    private SubDeviceType type;


    private List<EnergyCalculationType> energyCalculationTypes = new ArrayList<>();
    private List<HeatingCalculationType> heatingCalculationTypes = new ArrayList<>();
    private List<GasCalculationType> gasCalculationTypes = new ArrayList<>();
    private List<ChpCalculationType> chpCalculationTypes = new ArrayList<>(); //CHP

    public void addEnergyCalculationType(EnergyCalculationType energyCalculationType) {
        this.energyCalculationTypes.add(energyCalculationType);
    }

    public void addHeatingCalculationType(HeatingCalculationType heatingCalculationType) {
        this.heatingCalculationTypes.add(heatingCalculationType);
    }

    public void addGasCalculationType(GasCalculationType gasCalculationType) {
        this.gasCalculationTypes.add(gasCalculationType);
    }

    public void addChpCalculationType(ChpCalculationType chpCalculationType) {
        this.chpCalculationTypes.add(chpCalculationType);
    }


    public SubDevice(int startAddress, int slaveId, int registersQuantity, SubDeviceType type) {
        this.slaveId = slaveId;
        this.registersQuantity = registersQuantity;
        this.type = type;
        this.startAddress = startAddress;
    }
}

