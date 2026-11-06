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

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

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
        "dewey",
        "collaborators",
        "tags"
      })
  Optional<Book> findDetailById(@Param("id") Long id);

  boolean existsByIsbn(String isbn);

  boolean existsByIsbnAndIdNot(String isbn, Long id);

  @Query(
      """
      SELECT new com.nimbusphagia.library.catalog.BookLabelData(
          b.id, b.title, c.code, d.code, b.collectionVolume, b.seriesVolume)
      FROM Book b
      LEFT JOIN b.category c
      LEFT JOIN b.dewey d
      WHERE b.id IN :ids
      """)
  List<BookLabelData> findLabelDataByIdIn(@Param("ids") Set<Long> ids);
}
