package com.nimbusphagia.library.inventory.internal.repository;

import com.nimbusphagia.library.inventory.internal.entity.StockMovementLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementLineRepository extends JpaRepository<StockMovementLine, Long> {}
