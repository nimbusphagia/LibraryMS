package com.nimbusphagia.library.organization.internal.repository;

import com.nimbusphagia.library.organization.internal.entity.Institution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {

  interface LibraryScope {
    Long getInstitutionId();

    Long getOrganizationId();

    String getTimeZone();
  }

  Optional<Institution> findByIdAndOrganizationId(Long id, Long organizationId);

  List<Institution> findByOrganizationIdOrderByNameAsc(Long organizationId);

  List<Institution> findByOrganizationIdAndActiveOrderByNameAsc(
      Long organizationId, boolean active);

  boolean existsByOrganizationIdAndNameIgnoreCase(Long organizationId, String name);

  boolean existsByOrganizationIdAndNameIgnoreCaseAndIdNot(
      Long organizationId, String name, Long id);

  // Native so this module does not depend on the inventory's Library entity
  @Query(
      value =
          """
          SELECT i.id AS institutionId, i.organization_id AS organizationId,
                 i.time_zone AS timeZone
          FROM library l JOIN institution i ON i.id = l.institution_id
          WHERE l.id = :libraryId
          """,
      nativeQuery = true)
  Optional<LibraryScope> findScopeOfLibrary(@Param("libraryId") Long libraryId);
}
