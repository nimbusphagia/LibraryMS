package com.nimbusphagia.library.loan.internal.dto.visitorRole;

import com.nimbusphagia.library.loan.internal.entity.VisitorRole;

public record VisitorRoleResponse(
    Long id,
    String name,
    boolean requiresGrade,
    int maxDays,
    int maxActiveLoans,
    int overdueBlockThreshold,
    int graceDays) {
  public static VisitorRoleResponse fromEntity(VisitorRole role) {
    return new VisitorRoleResponse(
        role.getId(),
        role.getName(),
        role.isRequiresGrade(),
        role.getMaxDays(),
        role.getMaxActiveLoans(),
        role.getOverdueBlockThreshold(),
        role.getGraceDays());
  }
}
