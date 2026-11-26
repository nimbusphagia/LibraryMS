package com.nimbusphagia.library.loan.internal.dto.visitor;

import com.nimbusphagia.library.loan.internal.dto.schoolGrade.SchoolGradeResponse;
import com.nimbusphagia.library.loan.internal.dto.visitorRole.VisitorRoleResponse;
import com.nimbusphagia.library.loan.internal.entity.Reputation;
import com.nimbusphagia.library.loan.internal.entity.Visitor;

public record VisitorResponse(
    Long id,
    String name,
    VisitorRoleResponse role,
    SchoolGradeResponse grade,
    boolean active,
    Reputation reputation) {

  public static VisitorResponse fromEntity(Visitor visitor, Reputation reputation) {
    return new VisitorResponse(
        visitor.getId(),
        visitor.getName(),
        VisitorRoleResponse.fromEntity(visitor.getRole()),
        visitor.getGrade() == null ? null : SchoolGradeResponse.fromEntity(visitor.getGrade()),
        Boolean.TRUE.equals(visitor.getActive()),
        reputation);
  }
}
