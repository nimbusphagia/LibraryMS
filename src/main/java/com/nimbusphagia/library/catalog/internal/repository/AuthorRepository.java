package com.nimbusphagia.library.catalog.internal.repository;

import com.nimbusphagia.library.catalog.internal.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorRepository
    extends JpaRepository<Author, Long>, JpaSpecificationExecutor<Author> {

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM book WHERE author_id = :id OR illustrator_id = :id
            UNION ALL
            SELECT 1 FROM book_collaborator WHERE collaborator_id = :id
            UNION ALL
            SELECT 1 FROM series WHERE author_id = :id
          )
          """,
      nativeQuery = true)
  boolean isReferenced(@Param("id") Long id);
}
