package com.nimbusphagia.library_ms.catalog.internal.repository;

import com.nimbusphagia.library_ms.catalog.internal.entity.DeweyTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeweyTagRepository extends JpaRepository<DeweyTag, Long> {
  @Query(
      """
      SELECT t FROM DeweyTag t
      WHERE LOWER(t.code) LIKE LOWER(CONCAT('%', :search, '%'))
         OR LOWER(COALESCE(t.name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
      """)
  Page<DeweyTag> search(@Param("search") String search, Pageable pageable);
}
