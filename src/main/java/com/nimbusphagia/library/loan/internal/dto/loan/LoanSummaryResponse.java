package com.nimbusphagia.library.loan.internal.dto.loan;

import com.nimbusphagia.library.loan.internal.entity.LoanMovement;
import com.nimbusphagia.library.loan.internal.entity.LoanStatus;
import java.time.Instant;

public record LoanSummaryResponse(
    Long id,
    Long libraryId,
    Long visitorId,
    String visitorName,
    Long loanedById,
    String loanedByName,
    Instant createdAt,
    Instant expiresBy,
    LoanStatus status,
    long lineCount,
    long openLineCount,
    String notes) {

  public static LoanSummaryResponse fromEntity(
      LoanMovement m, long lineCount, long openLineCount, Instant now) {
    return new LoanSummaryResponse(
        m.getId(),
        m.getLibrary().getId(),
        m.getLoanedTo().getId(),
        m.getLoanedTo().getName(),
        m.getLoanedBy().getId(),
        m.getLoanedBy().getFullName(),
        m.getCreatedAt(),
        m.getExpiresBy(),
        LoanStatus.of(openLineCount, m.getExpiresBy(), now),
        lineCount,
        openLineCount,
        m.getNotes());
  }
}
