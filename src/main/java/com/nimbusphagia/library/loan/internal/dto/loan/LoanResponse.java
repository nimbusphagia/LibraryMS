package com.nimbusphagia.library.loan.internal.dto.loan;

import com.nimbusphagia.library.loan.internal.entity.LoanMovement;
import com.nimbusphagia.library.loan.internal.entity.LoanMovementLine;
import java.time.Instant;
import java.util.List;

public record LoanResponse(
    Long id,
    Long libraryId,
    Long visitorId,
    Instant createdAt,
    Instant expiresBy,
    String notes,
    List<LoanLineResponse> lines) {

  public static LoanResponse fromEntity(LoanMovement movement, List<LoanMovementLine> lines) {
    return new LoanResponse(
        movement.getId(),
        movement.getLibrary().getId(),
        movement.getLoanedTo().getId(),
        movement.getCreatedAt(),
        movement.getExpiresBy(),
        movement.getNotes(),
        lines.stream().map(LoanLineResponse::fromEntity).toList());
  }
}
