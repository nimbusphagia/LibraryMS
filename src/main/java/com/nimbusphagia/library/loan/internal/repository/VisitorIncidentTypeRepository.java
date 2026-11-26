package com.nimbusphagia.library.loan.internal.repository;

import com.nimbusphagia.library.loan.internal.entity.VisitorIncidentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitorIncidentTypeRepository extends JpaRepository<VisitorIncidentType, Long> {

  List<VisitorIncidentType> findByInstitutionIdOrderByNameAsc(Long institutionId);

  Optional<VisitorIncidentType> findByIdAndInstitutionId(Long id, Long institutionId);

  boolean existsByInstitutionIdAndNameIgnoreCase(Long institutionId, String name);

  boolean existsByInstitutionIdAndNameIgnoreCaseAndIdNot(
      Long institutionId, String name, Long id);
}
