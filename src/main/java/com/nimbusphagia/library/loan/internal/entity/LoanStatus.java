package com.nimbusphagia.library.loan.internal.entity;

import java.time.Instant;

public enum LoanStatus {
  OPEN,
  OVERDUE,
  CLOSED;

  public static LoanStatus of(long openLineCount, Instant expiresBy, Instant now) {
    if (openLineCount == 0) return CLOSED;
    return expiresBy.isBefore(now) ? OVERDUE : OPEN;
  }
}
