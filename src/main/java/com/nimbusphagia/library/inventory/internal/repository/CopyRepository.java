package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.inventory.internal.entity.CopyStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CopyRepository extends JpaRepository<Copy, Long>, JpaSpecificationExecutor<Copy> {

  @EntityGraph(attributePaths = {"library", "book", "shelf"})
  @Query("SELECT c FROM Copy c WHERE c.id = :id")
  Optional<Copy> findDetailedById(@Param("id") Long id);

  @Override
  @EntityGraph(attributePaths = {"shelf"})
  Page<Copy> findAll(Specification<Copy> spec, Pageable pageable);

  @Query(
      """
      SELECT COALESCE(MAX(c.iteration), 0) FROM Copy c
      WHERE c.library.id = :libraryId
        AND c.book.id = :bookId
        AND c.status IN :statuses
      """)
  int findMaxHeldIteration(
      @Param("libraryId") Long libraryId,
      @Param("bookId") Long bookId,
      @Param("statuses") Set<CopyStatus> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = {"shelf"})
  @Query("SELECT c FROM Copy c WHERE c.id IN :ids ORDER BY c.id")
  List<Copy> findAllByIdInForUpdate(@Param("ids") Set<Long> ids);

  boolean existsByLibraryIdAndBookIdAndIterationAndStatusIn(
      Long libraryId, Long bookId, Integer iteration, Set<CopyStatus> statuses);

  boolean existsByShelfId(Long shelfId);

  @EntityGraph(attributePaths = {"book", "placedBy"})
  List<Copy> findByLibraryIdAndStatusInOrderById(Long libraryId, Set<CopyStatus> statuses);

  List<Copy> findByLibraryIdAndBookIdAndStatusIn(
      Long libraryId, Long bookId, Set<CopyStatus> statuses);

  boolean existsByLibraryIdAndPlacedTextIgnoreCaseAndStatusIn(
      Long libraryId, String placedText, Set<CopyStatus> statuses);

  // A copy of another book on the shelves shows the text
  @Query(
      """
      SELECT COUNT(c) > 0 FROM Copy c
      WHERE c.library.id = :libraryId
        AND LOWER(c.placedText) = LOWER(:text)
        AND c.status IN :statuses
        AND c.book.id <> :bookId
      """)
  boolean isPlacedOnOtherBook(
      @Param("libraryId") Long libraryId,
      @Param("text") String text,
      @Param("bookId") Long bookId,
      @Param("statuses") Set<CopyStatus> statuses);
}
