package com.nimbusphagia.library.loan.internal.entity;

public enum GradeLevel {
  PRIMARY(6),
  SECONDARY(5);

  private final int lastGrade;

  GradeLevel(int lastGrade) {
    this.lastGrade = lastGrade;
  }

  public int lastGrade() {
    return lastGrade;
  }
}
