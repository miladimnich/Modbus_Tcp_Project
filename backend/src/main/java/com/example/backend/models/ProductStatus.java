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

    @Id
    @Column(name = "ObjektNr")
    private Integer objectNumber;

    @Column(name = "ProduktStatus")
    private Short productStatus;

    @Column(name = "SerienNummer")
    private Integer serialNumber;
}
