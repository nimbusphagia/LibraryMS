package com.nimbusphagia.library.catalog.internal.repository;

import com.nimbusphagia.library.catalog.internal.entity.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionRepository
    extends JpaRepository<Collection, Long>, JpaSpecificationExecutor<Collection> {
  Optional<Collection> findByIdAndPublisherId(Long id, Long publisherId);

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM book WHERE collection_id = :id
          )
          """,
      nativeQuery = true)
  boolean isReferenced(@Param("id") Long id);
}
