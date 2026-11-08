package com.nimbusphagia.library.loan.internal.dto.loan;

import com.nimbusphagia.library.inventory.internal.entity.CopyCondition;
import com.nimbusphagia.library.loan.internal.entity.LoanOutcome;
import java.time.Instant;

public record LoanLineDetailResponse(
    Long id,
    Long copyId,
    Long bookId,
    String bookTitle,
    Integer iteration,
    CopyCondition condition,
    Instant resolvedAt,
    Long resolvedById,
    String resolvedByName,
    LoanOutcome outcome) {}
