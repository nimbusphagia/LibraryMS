package com.nimbusphagia.library.loan.internal.dto.loan;

import com.nimbusphagia.library.loan.internal.entity.LoanMovementLine;
import com.nimbusphagia.library.loan.internal.entity.LoanOutcome;
import java.time.Instant;

public record LoanLineResponse(Long id, Long copyId, Instant resolvedAt, LoanOutcome outcome) {

  public static LoanLineResponse fromEntity(LoanMovementLine line) {
    return new LoanLineResponse(
        line.getId(), line.getCopy().getId(), line.getResolvedAt(), line.getOutcome());
  }
}
