package com.example.backend.enums;

public enum Maschinentype {
  ASV_20(19, 20),
  ASV_21(20, 60),
  ASV_30(30, 80),
  ASV_40(40, 100);
  private final int min;
  private final int max;

  Maschinentype(int min, int max) {
    this.min = min;
    this.max = max;
  }

  public int getMin() {
    return min;
  }

  public int getMax() {
    return max;
  }
}