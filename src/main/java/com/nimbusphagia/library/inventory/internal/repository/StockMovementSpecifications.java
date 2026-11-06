package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.dto.stock.StockMovementFilter;
import com.nimbusphagia.library.inventory.internal.entity.StockMovement;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class StockMovementSpecifications {
  private StockMovementSpecifications() {}

  public static Specification<StockMovement> from(Long libraryId, StockMovementFilter f) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("library").get("id"), libraryId));
      if (f.type() != null) predicates.add(cb.equal(root.get("type"), f.type()));
      if (f.reason() != null) predicates.add(cb.equal(root.get("reason"), f.reason()));
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }
}
