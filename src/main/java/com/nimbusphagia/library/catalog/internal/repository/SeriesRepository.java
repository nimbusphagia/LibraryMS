package com.nimbusphagia.library.catalog.internal.repository;

import com.nimbusphagia.library.catalog.internal.entity.Series;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeriesRepository
    extends JpaRepository<Series, Long>, JpaSpecificationExecutor<Series> {
  Optional<Series> findByIdAndAuthorId(Long id, Long authorId);

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
