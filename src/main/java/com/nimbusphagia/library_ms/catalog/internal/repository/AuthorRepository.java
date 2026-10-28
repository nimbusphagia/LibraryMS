package com.nimbusphagia.library_ms.catalog.internal.repository;

import com.nimbusphagia.library_ms.catalog.internal.entity.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorRepository extends JpaRepository<Author, Long> {

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM author
            WHERE LOWER(first_name) = LOWER(:firstName)
              AND LOWER(last_name) IS NOT DISTINCT FROM LOWER(CAST(:lastName AS text))
          )
          """,
      nativeQuery = true)
  boolean existsByFullName(
      @Param("firstName") String firstName, @Param("lastName") String lastName);

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM author
            WHERE LOWER(first_name) = LOWER(:firstName)
              AND LOWER(last_name) IS NOT DISTINCT FROM LOWER(CAST(:lastName AS text))
              AND id <> :id
          )
          """,
      nativeQuery = true)
  boolean existsByFullNameAndIdNot(
      @Param("firstName") String firstName,
      @Param("lastName") String lastName,
      @Param("id") Long id);

  @Query(
      """
      SELECT a FROM Author a
      WHERE LOWER(CONCAT(a.firstName, ' ', COALESCE(a.lastName, '')))
            LIKE LOWER(CONCAT('%', :search, '%'))
      """)
  Page<Author> search(@Param("search") String search, Pageable pageable);

  @Query(
      value =
          """
          SELECT EXISTS (
            SELECT 1 FROM book WHERE author_id = :id OR illustrator_id = :id
            UNION ALL
            SELECT 1 FROM book_collaborator WHERE collaborator_id = :id
          )
          """,
      nativeQuery = true)
  boolean isReferenced(@Param("id") Long id);
}
