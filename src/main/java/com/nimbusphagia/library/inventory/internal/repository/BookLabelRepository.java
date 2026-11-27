package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.BookLabel;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookLabelRepository extends JpaRepository<BookLabel, Long> {

  @EntityGraph(attributePaths = {"library", "book", "scheme"})
  Optional<BookLabel> findByLibraryIdAndBookId(Long libraryId, Long bookId);

  @EntityGraph(attributePaths = {"library", "book", "scheme"})
  List<BookLabel> findByBookIdIn(Collection<Long> bookIds);

  @EntityGraph(attributePaths = {"library", "book", "scheme"})
  List<BookLabel> findByLibraryIdAndBookIdIn(Long libraryId, Collection<Long> bookIds);

  @EntityGraph(attributePaths = {"library", "book", "scheme"})
  List<BookLabel> findByLibraryId(Long libraryId);

  @EntityGraph(attributePaths = {"library", "book", "scheme"})
  List<BookLabel> findByLibraryLabelSchemeId(Long schemeId);

  // Another book of the library already has the code
  boolean existsByLibraryIdAndCodeIgnoreCaseAndBookIdNot(Long libraryId, String code, Long bookId);

  boolean existsBySchemeId(Long schemeId);

  // Parts that labels stuck on these books' copies take from catalog data
  @Query(
      value =
          """
          SELECT DISTINCT p.key FROM book_label b, jsonb_each(b.parts) p
          WHERE b.book_id IN (:bookIds)
            AND p.value ->> 'origin' = 'DERIVED'
            AND EXISTS (
              SELECT 1 FROM copy c
              WHERE c.library_id = b.library_id AND c.book_id = b.book_id
                AND c.placed_text IS NOT NULL AND c.status IN ('AVAILABLE', 'ON_LOAN'))
          """,
      nativeQuery = true)
  List<String> findDerivedPartsOfPlaced(@Param("bookIds") Collection<Long> bookIds);

  // Malaga codes this library gave the author's books, same Dewey number first
  @Query(
      value =
          """
          SELECT bl.parts -> 'AUTHOR_MALAGA' ->> 'value' FROM book_label bl
          JOIN book b ON b.id = bl.book_id
          WHERE bl.library_id = :libraryId
            AND b.author_id = :authorId
            AND bl.parts -> 'AUTHOR_MALAGA' IS NOT NULL
          ORDER BY b.dewey_number IS NOT DISTINCT FROM CAST(:deweyNumber AS VARCHAR) DESC,
                   bl.id DESC
          """,
      nativeQuery = true)
  List<String> findMalagaCodesOfAuthor(
      @Param("libraryId") Long libraryId,
      @Param("authorId") Long authorId,
      @Param("deweyNumber") String deweyNumber);
}
