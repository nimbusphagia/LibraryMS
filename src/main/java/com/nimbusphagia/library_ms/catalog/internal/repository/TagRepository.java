package com.nimbusphagia.library_ms.catalog.internal.repository;

import com.nimbusphagia.library_ms.catalog.internal.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<Tag, Long> {
  @Query("SELECT COUNT(t) > 0 FROM Tag t WHERE LOWER(t.name) = LOWER(:name)")
  boolean existsByNameIgnoreCase(@Param("name") String name);

  @Query(
      """
      SELECT COUNT(t) > 0 FROM Tag t
      WHERE LOWER(t.name) = LOWER(:name) AND t.id <> :id
      """)
  boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);

  Page<Tag> findByNameContainingIgnoreCase(String name, Pageable pageable);

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM book_tag WHERE tag_id = :id
          )
          """,
      nativeQuery = true)
  boolean isReferenced(@Param("id") Long id);
}
