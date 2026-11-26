package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.Library;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryRepository extends JpaRepository<Library, Long> {

  boolean existsByInstitutionIdAndNameIgnoreCase(Long institutionId, String name);

  boolean existsByInstitutionIdAndNameIgnoreCaseAndIdNot(Long institutionId, String name, Long id);

  @Query(
      """
      SELECT l FROM Library l
      WHERE l.institutionId IN
            (SELECT i.id FROM Institution i WHERE i.organizationId = :organizationId)
        AND (:institutionId IS NULL OR l.institutionId = :institutionId)
        AND (:active IS NULL OR l.active = :active)
      ORDER BY l.name
      """)
  List<Library> search(
      @Param("organizationId") Long organizationId,
      @Param("institutionId") Long institutionId,
      @Param("active") Boolean active);
}
