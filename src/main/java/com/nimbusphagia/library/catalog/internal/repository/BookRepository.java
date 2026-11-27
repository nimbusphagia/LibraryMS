package com.nimbusphagia.library.catalog.internal.repository;

import com.nimbusphagia.library.catalog.BookLabelData;
import com.nimbusphagia.library.catalog.internal.entity.Book;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository
    extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

  @Override
  @EntityGraph(attributePaths = {"author", "publisher"})
  Page<Book> findAll(Specification<Book> spec, Pageable pageable);

  @Query("SELECT b FROM Book b WHERE b.id = :id")
  @EntityGraph(
      attributePaths = {
        "author",
        "illustrator",
        "publisher",
        "collection",
        "series",
        "category",
        "subcategory",
        "collaborators",
        "tags"
      })
  Optional<Book> findDetailById(@Param("id") Long id);

  @Query("SELECT b.id FROM Book b WHERE b.category.id = :categoryId")
  Set<Long> findIdsByCategoryId(@Param("categoryId") Long categoryId);

  @Query("SELECT b.id FROM Book b WHERE b.subcategory.id = :subcategoryId")
  Set<Long> findIdsBySubcategoryId(@Param("subcategoryId") Long subcategoryId);

  @Query(
      """
      SELECT new com.nimbusphagia.library.catalog.BookLabelData(
          b.id, b.title, a.id, a.firstName, a.lastName, c.code, s.code, b.deweyNumber,
          b.collectionVolume, b.seriesVolume)
      FROM Book b
      LEFT JOIN b.author a
      LEFT JOIN b.category c
      LEFT JOIN b.subcategory s
      WHERE b.id IN :ids
      """)
  List<BookLabelData> findLabelDataByIdIn(@Param("ids") Set<Long> ids);

  @Query(
      """
      SELECT b.id FROM Book b
      WHERE b.id IN :ids
        AND b.organizationId = :organizationId
        AND (b.institutionId IS NULL OR b.institutionId = :institutionId)
      """)
  Set<Long> findIdsVisibleTo(
      @Param("ids") Set<Long> ids,
      @Param("organizationId") Long organizationId,
      @Param("institutionId") Long institutionId);
}
