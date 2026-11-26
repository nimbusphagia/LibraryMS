package com.nimbusphagia.library.loan.internal.dto.visitorRole;

import com.nimbusphagia.library.loan.internal.entity.VisitorRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record VisitorRoleRequest(
    @NotBlank(message = "Role name is missing") @Size(max = 255) String name,
    boolean requiresGrade,
    @NotNull(message = "Maximum days is missing") @Positive Integer maxDays,
    @NotNull(message = "Maximum active loans is missing") @Positive Integer maxActiveLoans,
    @NotNull(message = "Overdue block threshold is missing") @Positive
        Integer overdueBlockThreshold,
    @NotNull(message = "Grace days is missing") @PositiveOrZero Integer graceDays) {

  public VisitorRoleRequest {
    name = name == null ? null : name.strip();
  }

  public VisitorRole toEntity(Long organizationId, Long institutionId) {
    return replace(
        VisitorRole.builder().organizationId(organizationId).institutionId(institutionId).build());
  }

  public VisitorRole replace(VisitorRole role) {
    role.setName(name);
    role.setRequiresGrade(requiresGrade);
    role.setMaxDays(maxDays);
    role.setMaxActiveLoans(maxActiveLoans);
    role.setOverdueBlockThreshold(overdueBlockThreshold);
    role.setGraceDays(graceDays);
    return role;
  }
}
