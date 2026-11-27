package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.catalog.LabelledBooks;
import com.nimbusphagia.library.inventory.internal.repository.LabelRepository;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class LabelledBooksImpl implements LabelledBooks {
  private final LabelRepository labelRepository;

  @Override
  @Transactional(readOnly = true)
  public Set<LabelPart> derivedPartsInUse(Collection<Long> bookIds) {
    Set<LabelPart> parts = EnumSet.noneOf(LabelPart.class);
    if (!bookIds.isEmpty()) {
      labelRepository
          .findDerivedPartsOfPrinted(bookIds)
          .forEach(part -> parts.add(LabelPart.valueOf(part)));
    }
    return parts;
  }
}
