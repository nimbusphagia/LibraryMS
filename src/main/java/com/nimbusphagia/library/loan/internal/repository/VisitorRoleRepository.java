package com.nimbusphagia.library.loan.internal.repository;

import com.nimbusphagia.library.loan.internal.entity.VisitorRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// A null institutionId selects the organization's templates
public interface VisitorRoleRepository extends JpaRepository<VisitorRole, Long> {

  List<VisitorRole> findByOrganizationIdAndInstitutionIdOrderByIdAsc(
      Long organizationId, Long institutionId);

  Optional<VisitorRole> findByIdAndOrganizationIdAndInstitutionId(
      Long id, Long organizationId, Long institutionId);

  boolean existsByOrganizationIdAndInstitutionIdAndNameIgnoreCase(
      Long organizationId, Long institutionId, String name);

  boolean existsByOrganizationIdAndInstitutionIdAndNameIgnoreCaseAndIdNot(
      Long organizationId, Long institutionId, String name, Long id);
}
