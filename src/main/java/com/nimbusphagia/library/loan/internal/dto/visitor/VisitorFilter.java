package com.nimbusphagia.library.loan.internal.dto.visitor;

public record VisitorFilter(String search, Long roleId, Long gradeId, Boolean active) {

  public VisitorFilter {
    search = search == null || search.isBlank() ? null : search.strip();
  }
}
