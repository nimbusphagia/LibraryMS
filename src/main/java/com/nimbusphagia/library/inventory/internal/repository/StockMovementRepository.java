package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.StockMovement;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockMovementRepository
    extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

  @Override
  @EntityGraph(attributePaths = {"signedBy"})
  Page<StockMovement> findAll(Specification<StockMovement> spec, Pageable pageable);

  @EntityGraph(attributePaths = {"signedBy"})
  Optional<StockMovement> findDetailedById(Long id);
}
