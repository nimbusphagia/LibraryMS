package com.nimbusphagia.library.loan.internal.dto.loan;

import com.nimbusphagia.library.loan.internal.entity.LoanStatus;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record LoanFilter(
    Long visitorId,
    String visitorName,
    LoanStatus status,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo) {

  public LoanFilter {
    visitorName = visitorName == null || visitorName.isBlank() ? null : visitorName.strip();
  }
}
