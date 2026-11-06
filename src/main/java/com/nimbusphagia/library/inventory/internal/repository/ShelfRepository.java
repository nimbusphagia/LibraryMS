package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.Shelf;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShelfRepository extends JpaRepository<Shelf, Long> {

  Optional<Shelf> findByIdAndLibraryId(Long id, Long libraryId);

  Optional<Shelf> findByLibraryIdAndDefaultShelfTrue(Long libraryId);
}
