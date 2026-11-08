package com.nimbusphagia.library.loan.internal.dto.loan;

import com.nimbusphagia.library.loan.internal.dto.visitor.VisitorResponse;
import com.nimbusphagia.library.loan.internal.entity.LoanStatus;
import java.time.Instant;
import java.util.List;

public record LoanDetailResponse(
    Long id,
    Long libraryId,
    VisitorResponse visitor,
    Long loanedById,
    String loanedByName,
    Instant createdAt,
    Instant expiresBy,
    LoanStatus status,
    String notes,
    List<LoanLineDetailResponse> lines) {}
