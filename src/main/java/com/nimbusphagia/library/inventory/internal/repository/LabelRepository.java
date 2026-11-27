package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.Label;
import com.nimbusphagia.library.inventory.internal.entity.PrintStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LabelRepository extends JpaRepository<Label, Long> {

  boolean existsByCopyId(Long copyId);

  @Query(
      value =
          """
          SELECT DISTINCT p.key FROM label l, jsonb_each(l.parts) p
          WHERE l.book_id IN (:bookIds)
            AND l.retired_at IS NULL
            AND l.print_status = 'PRINTED'
            AND p.value ->> 'origin' = 'DERIVED'
          """,
      nativeQuery = true)
  List<String> findDerivedPartsOfPrinted(@Param("bookIds") Collection<Long> bookIds);

  @EntityGraph(attributePaths = {"library", "book", "printedBy"})
  Optional<Label> findByCopyId(Long copyId);

  @EntityGraph(attributePaths = {"library", "book", "printedBy"})
  List<Label> findByIdIn(Set<Long> ids);

  @EntityGraph(attributePaths = {"library", "book", "printedBy"})
  @Query(
      """
      SELECT l FROM Label l
      WHERE l.library.id = :libraryId
        AND l.printStatus = :status
        AND l.retiredAt IS NULL
      ORDER BY l.id
      """)
  List<Label> findActiveByLibraryAndPrintStatus(
      @Param("libraryId") Long libraryId, @Param("status") PrintStatus status);

  Optional<Label> findFirstByLibraryIdAndBookIdAndRetiredAtIsNullOrderByIdDesc(
      Long libraryId, Long bookId);

  List<Label> findByCopyIdInAndRetiredAtIsNull(Collection<Long> copyIds);

  @Query(
      """
      SELECT COUNT(l) > 0 FROM Label l
      WHERE l.library.id = :libraryId
        AND LOWER(l.text) = LOWER(:text)
        AND l.retiredAt IS NULL
        AND l.printStatus <> com.nimbusphagia.library.inventory.internal.entity.PrintStatus.OUTDATED
        AND (:labelId IS NULL OR l.id <> :labelId)
      """)
  boolean isTextTaken(
      @Param("libraryId") Long libraryId,
      @Param("text") String text,
      @Param("labelId") Long labelId);

  @EntityGraph(attributePaths = {"library", "book"})
  List<Label> findByBookIdInAndRetiredAtIsNull(Collection<Long> bookIds);

  @EntityGraph(attributePaths = {"library", "book"})
  List<Label> findByLibraryLabelSchemeIdAndRetiredAtIsNull(Long schemeId);

  @EntityGraph(attributePaths = {"library", "book"})
  List<Label> findByLibraryIdAndRetiredAtIsNull(Long libraryId);
}
