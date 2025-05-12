package com.example.backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ew_erw_werksprotokolle")
public class Werksprotokolle {

  @Column(name = "ObjektNr")
  @Id
  private Integer objektNr;

  @Column(name = "DirektBezug1Nr")
  private Integer direktBezug1Nr;

  @Column(name = "MessungAnfangUhrzeit")
  private LocalDateTime messungAnfangUhrzeit;

  @Column(name = "MessungEndeUhrzeit")
  private LocalDateTime messungEndeUhrzeit;

  @Column(name = "MessungAnfangBetriebsstundenInSekunden")
  private Integer messungAnfangBetriebsstundenInSekunden;

  @Column(name = "MessungAnfangStarts")
  private Integer messungAnfangStarts;

  @Column(name = "MessungAnfangkWh")
  private Double messungAnfangkWh;

  @Column(name = "MessungAnfangLeistung")
  private Double messungAnfangLeistung;

  @Column(name = "MessungAnfangWaerme")
  private Double messungAnfangWaerme;

  @Column(name = "MessungAnfangGas")
  private Double messungAnfangGas;

  @Column(name = "MessungAnfangGastemperatur")
  private Double messungAnfangGastemperatur;

  @Column(name = "MessungAnfangGasfliessdruck")
  private Double messungAnfangGasfliessdruck;

  @Column(name = "MessungAnfangLuftdruck")
  private Double messungAnfangLuftdruck;

  @Column(name = "MessungAnfangLufttemperatur")
  private Double messungAnfangLufttemperatur;

  @Column(name = "MessungAnfangTemp1WMZ")
  private Double messungAnfangTemp1WMZ;

  @Column(name = "MessungAnfangTemp2WMZ")
  private Double messungAnfangTemp2WMZ;

  @Column(name = "MessungAnfangVolumenstromWMZ")
  private Double messungAnfangVolumenstromWMZ;

  @Column(name = "MessungAnfangTemp1")
  private Double messungAnfangTemp1;

  @Column(name = "MessungAnfangTemp2")
  private Double messungAnfangTemp2;

  @Column(name = "MessungAnfangTemp3")
  private Double messungAnfangTemp3;

  @Column(name = "MessungAnfangTemp4")
  private Double messungAnfangTemp4;

  @Column(name = "MessungAnfangTemp5")
  private Double messungAnfangTemp5;

  @Column(name = "MessungAnfangTemp6")
  private Double messungAnfangTemp6;

  @Column(name = "MessungAnfangTemp7")
  private Double MessungAnfangTemp7;

  @Column(name = "MessungAnfangTemp8")
  private Double messungAnfangTemp8;

  @Column(name = "MessungAnfangTemp9")
  private Double messungAnfangTemp9;

  @Column(name = "MessungAnfangTemp10")
  private Double messungAnfangTemp10;

  @Column(name = "MessungEndeBetriebsstundenInSekunden")
  private Double messungEndeBetriebsstundenInSekunden;

  @Column(name = "MessungEndeStarts")
  private Double messungEndeStarts;

  @Column(name = "MessungEndekWh")
  private Double messungEndekWh;

  @Column(name = "MessungEndeLeistung")
  private Double messungEndeLeistung;

  @Column(name = "MessungEndeWaerme")
  private Double messungEndeWaerme;

  @Column(name = "MessungEndeGas")
  private Double messungEndeGas;

  @Column(name = "MessungEndeGastemperatur")
  private Double messungEndeGastemperatur;

  @Column(name = "MessungEndeGasfliessdruck")
  private Double messungEndeGasfliessdruck;


  @Column(name = "MessungEndeLuftdruck")
  private Double messungEndeLuftdruck;

  @Column(name = "MessungEndeLufttemperatur")
  private Double messungEndeLufttemperatur;

  @Column(name = "MessungEndeTemp1WMZ")
  private Double messungEndeTemp1WMZ;

  @Column(name = "MessungEndeTemp2WMZ")
  private Double messungEndeTemp2WMZ;

  @Column(name = "MessungEndeVolumenstromWMZ")
  private Double messungEndeVolumenstromWMZ;

  @Column(name = "MessungEndeTemp1")
  private Double messungEndeTemp1;

  @Column(name = "MessungEndeTemp2")
  private Double messungEndeTemp2;

  @Column(name = "MessungEndeTemp3")
  private Double messungEndeTemp3;

  @Column(name = "MessungEndeTemp4")
  private Double messungEndeTemp4;

  @Column(name = "MessungEndeTemp5")
  private Double messungEndeTemp5;

  @Column(name = "MessungEndeTemp6")
  private Double messungEndeTemp6;

  @Column(name = "MessungEndeTemp7")
  private Double messungEndeTemp7;

  @Column(name = "MessungEndeTemp8")
  private Double messungEndeTemp8;

  @Column(name = "MessungEndeTemp9")
  private Double messungEndeTemp9;

  @Column(name = "MessungEndeTemp10")
  private Double messungEndeTemp10;
}
