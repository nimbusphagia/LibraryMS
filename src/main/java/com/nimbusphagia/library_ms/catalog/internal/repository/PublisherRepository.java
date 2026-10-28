package com.nimbusphagia.library_ms.catalog.internal.repository;

import com.nimbusphagia.library_ms.catalog.internal.entity.Publisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {
  @Query("SELECT COUNT(p) > 0 FROM Publisher p WHERE LOWER(p.name) = LOWER(:name)")
  boolean existsByNameIgnoreCase(@Param("name") String name);

  @Query(
      """
      SELECT COUNT(p) > 0 FROM Publisher p
      WHERE LOWER(p.name) = LOWER(:name) AND p.id <> :id
      """)
  boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);

  Page<Publisher> findByNameContainingIgnoreCase(String name, Pageable pageable);

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM book WHERE publisher_id = :id
            UNION ALL
            SELECT 1 FROM collection WHERE publisher_id = :id
          )
          """,
      nativeQuery = true)
  boolean isReferenced(@Param("id") Long id);
}
