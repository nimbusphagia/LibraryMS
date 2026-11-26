package com.nimbusphagia.library.catalog.internal.repository;

import com.nimbusphagia.library.catalog.internal.entity.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublisherRepository
    extends JpaRepository<Publisher, Long>, JpaSpecificationExecutor<Publisher> {

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
