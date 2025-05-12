package com.example.backend.models;

import com.example.backend.enums.BhkwCalculationType;
import com.example.backend.enums.EnergyCalculationType;
import com.example.backend.enums.GasCalculationType;
import com.example.backend.enums.HeatingCalculationType;
import com.example.backend.enums.SubDeviceType;
import java.util.ArrayList;
import java.util.List;
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


  @Getter
  private List<EnergyCalculationType> energyCalculationTypes = new ArrayList<>();
  @Getter
  private List<HeatingCalculationType> heatingCalculationTypes = new ArrayList<>();
  @Getter
  private List<GasCalculationType> gasCalculationType = new ArrayList<>();
  @Getter
  private List<BhkwCalculationType> bhkwCalculationTypes = new ArrayList<>();

  public void addEnergyCalculationType(EnergyCalculationType energyCalculationType) {
    this.energyCalculationTypes.add(energyCalculationType);
  }

  public void addHeatingCalculationType(HeatingCalculationType heatingCalculationType) {
    this.heatingCalculationTypes.add(heatingCalculationType);
  }

  public void addGasCalculationType(GasCalculationType gasCalculationType) {
    this.gasCalculationType.add(gasCalculationType);
  }
  public void addBhkwCalculationType(BhkwCalculationType bhkwCalculationType) {
    this.bhkwCalculationTypes.add(bhkwCalculationType);
  }


  public SubDevice(int startAddress, int slaveId, int registersQuantity, SubDeviceType type) {

    this.slaveId = slaveId;
    this.registersQuantity = registersQuantity;
    this.type = type;
    this.startAddress = startAddress;
  }
}
