package com.nimbusphagia.library.loan.internal.dto.visitorRole;

import com.nimbusphagia.library.loan.internal.entity.VisitorRole;

public record VisitorRoleResponse(Long id, String code, String name, int maxDays) {
  public static VisitorRoleResponse fromEntity(VisitorRole role) {
    return new VisitorRoleResponse(role.getId(), role.getCode(), role.getName(), role.getMaxDays());
  }
}
