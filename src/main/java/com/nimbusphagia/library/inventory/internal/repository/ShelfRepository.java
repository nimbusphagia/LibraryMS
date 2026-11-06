package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.Shelf;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShelfRepository extends JpaRepository<Shelf, Long> {

  Optional<Shelf> findByIdAndLibraryId(Long id, Long libraryId);

  Optional<Shelf> findByLibraryIdAndDefaultShelfTrue(Long libraryId);

  List<Shelf> findByLibraryIdOrderByDefaultShelfDescNameAsc(Long libraryId);

  boolean existsByLibraryIdAndNameIgnoreCase(Long libraryId, String name);

  boolean existsByLibraryIdAndNameIgnoreCaseAndIdNot(Long libraryId, String name, Long id);
}
