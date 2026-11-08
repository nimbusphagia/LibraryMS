package com.nimbusphagia.library.loan.internal.service;

import com.nimbusphagia.library.loan.internal.dto.schoolGrade.SchoolGradeResponse;
import com.nimbusphagia.library.loan.internal.dto.visitorRole.VisitorRoleResponse;
import com.nimbusphagia.library.loan.internal.repository.SchoolGradeRepository;
import com.nimbusphagia.library.loan.internal.repository.VisitorRoleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VisitorCatalogService {
  private final VisitorRoleRepository roleRepository;
  private final SchoolGradeRepository gradeRepository;

  @Transactional(readOnly = true)
  public List<VisitorRoleResponse> findRoles() {
    return roleRepository.findAllByOrderByIdAsc().stream()
        .map(VisitorRoleResponse::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<SchoolGradeResponse> findGrades() {
    return gradeRepository.findAllByOrderByLevelAscGradeAsc().stream()
        .map(SchoolGradeResponse::fromEntity)
        .toList();
  }
}
