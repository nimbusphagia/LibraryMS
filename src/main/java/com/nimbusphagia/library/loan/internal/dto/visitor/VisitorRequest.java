package com.nimbusphagia.library.loan.internal.dto.visitor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VisitorRequest(
    @NotBlank(message = "Visitor name is missing") @Size(max = 255) String name,
    @NotNull(message = "Visitor role is missing") Long roleId,
    Long gradeId) {

  public VisitorRequest {
    name = name == null ? null : name.strip();
  }
}
