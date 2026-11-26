package com.nimbusphagia.library.catalog.internal.repository;

import com.nimbusphagia.library.catalog.internal.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository
    extends JpaRepository<Tag, Long>, JpaSpecificationExecutor<Tag> {

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
