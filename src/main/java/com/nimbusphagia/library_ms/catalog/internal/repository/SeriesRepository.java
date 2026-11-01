package com.nimbusphagia.library_ms.catalog.internal.repository;

import com.nimbusphagia.library_ms.catalog.internal.entity.Series;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeriesRepository extends JpaRepository<Series, Long> {
  Optional<Series> findByIdAndAuthorId(Long id, Long authorId);

  @Query(
      """
      SELECT COUNT(s) > 0 FROM Series s
      WHERE s.author.id = :authorId AND LOWER(s.name) = LOWER(:name)
      """)
  boolean existsByAuthorAndName(@Param("authorId") Long authorId, @Param("name") String name);

  @Query(
      """
      SELECT COUNT(s) > 0 FROM Series s
      WHERE s.author.id = :authorId AND LOWER(s.name) = LOWER(:name) AND s.id <> :id
      """)
  boolean existsByAuthorAndNameAndIdNot(
      @Param("authorId") Long authorId, @Param("name") String name, @Param("id") Long id);

  Page<Series> findByAuthorId(Long authorId, Pageable pageable);

  Page<Series> findByAuthorIdAndNameContainingIgnoreCase(
      Long authorId, String name, Pageable pageable);

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM book WHERE series_id = :id
          )
          """,
      nativeQuery = true)
  boolean isReferenced(@Param("id") Long id);
}
