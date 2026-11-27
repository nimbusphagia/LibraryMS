package com.nimbusphagia.library.catalog.internal.repository;

import com.nimbusphagia.library.catalog.internal.entity.MalagaEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MalagaEntryRepository extends JpaRepository<MalagaEntry, Long> {
  List<MalagaEntry> findByLetterOrderById(String letter);
}
