package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.catalog.LabelledBooks;
import com.nimbusphagia.library.inventory.internal.repository.LabelRepository;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class LabelledBooksImpl implements LabelledBooks {
  private final LabelRepository labelRepository;

  @Override
  @Transactional(readOnly = true)
  public boolean anyLabelled(Collection<Long> bookIds) {
    return !bookIds.isEmpty() && labelRepository.existsByBookIdInAndRetiredAtIsNull(bookIds);
  }
}
