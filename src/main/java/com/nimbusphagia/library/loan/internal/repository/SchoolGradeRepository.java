package com.nimbusphagia.library.loan.internal.repository;

import com.nimbusphagia.library.loan.internal.entity.GradeLevel;
import com.nimbusphagia.library.loan.internal.entity.SchoolGrade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// A null institutionId selects the organization's templates
public interface SchoolGradeRepository extends JpaRepository<SchoolGrade, Long> {

  List<SchoolGrade> findByOrganizationIdAndInstitutionIdOrderByLevelAscGradeAsc(
      Long organizationId, Long institutionId);

  Optional<SchoolGrade> findByIdAndOrganizationIdAndInstitutionId(
      Long id, Long organizationId, Long institutionId);

  boolean existsByOrganizationIdAndInstitutionIdAndLevelAndGrade(
      Long organizationId, Long institutionId, GradeLevel level, Integer grade);
}
