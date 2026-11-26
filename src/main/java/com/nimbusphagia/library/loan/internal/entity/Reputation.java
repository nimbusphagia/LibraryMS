package com.nimbusphagia.library.loan.internal.entity;

// Same rules as NativeLMS, except that discarded copies weigh as damaged and incidents add their
// type's weight, counting as serious like a loss when their type says so
public enum Reputation {
  NEW,
  RELIABLE,
  REGULAR,
  AT_RISK;

  public static final int HISTORY_MONTHS = 12;
  private static final int MIN_HISTORY = 3;

  public static Reputation of(
      long resolved,
      long late,
      long damaged,
      long lost,
      long incidentPoints,
      long seriousIncidents) {
    long serious = lost + seriousIncidents;
    long points = late + damaged * 2 + lost * 4 + incidentPoints;
    double score = resolved == 0 ? 0 : points / (double) resolved;
    if (serious >= 3 || (serious >= 2 && score >= 1.5)) return AT_RISK;
    if (resolved < MIN_HISTORY) return NEW;
    return score >= 0.5 ? REGULAR : RELIABLE;
  }
}
