package com.nimbusphagia.library.loan.internal.dto.schoolGrade;

import com.nimbusphagia.library.loan.internal.entity.GradeLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SchoolGradeRequest(
    @NotNull(message = "Level is missing") GradeLevel level,
    @NotNull(message = "Grade is missing") @Positive Integer grade) {}
