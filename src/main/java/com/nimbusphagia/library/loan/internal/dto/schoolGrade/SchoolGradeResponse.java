package com.nimbusphagia.library.loan.internal.dto.schoolGrade;

import com.nimbusphagia.library.loan.internal.entity.GradeLevel;
import com.nimbusphagia.library.loan.internal.entity.SchoolGrade;

public record SchoolGradeResponse(Long id, GradeLevel level, int grade) {
  public static SchoolGradeResponse fromEntity(SchoolGrade schoolGrade) {
    return new SchoolGradeResponse(
        schoolGrade.getId(), schoolGrade.getLevel(), schoolGrade.getGrade());
  }
}
