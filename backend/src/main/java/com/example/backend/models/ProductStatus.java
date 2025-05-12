package com.example.backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ew_erw_produkte")  // Name of your existing table
public class ProductStatus {

  @Setter
  @Id
  @Column(name = "ObjektNr")
  private Integer objektNr;

  @Column(name = "ProduktStatus")
  private Short produktStatus;

  @Setter
  @Getter
  @Column(name = "SerienNummer")
  private Integer serienNummer;
}