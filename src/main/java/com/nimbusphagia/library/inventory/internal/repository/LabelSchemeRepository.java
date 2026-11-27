package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.LabelScheme;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LabelSchemeRepository extends JpaRepository<LabelScheme, Long> {

  // Templates first, then the organization's own schemes
  @Query(
      """
      SELECT s FROM LabelScheme s
      WHERE s.organizationId IS NULL OR s.organizationId = :organizationId
      ORDER BY s.organizationId NULLS FIRST, s.name
      """)
  List<LabelScheme> findUsableBy(@Param("organizationId") Long organizationId);

  Optional<LabelScheme> findByOrganizationIdIsNullAndName(String name);

  boolean existsByOrganizationIdAndNameIgnoreCaseAndIdNot(
      Long organizationId, String name, Long id);
}
