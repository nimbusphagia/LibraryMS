package com.nimbusphagia.library.catalog.internal.service;

import com.nimbusphagia.library.catalog.LabelDataChanged;
import com.nimbusphagia.library.catalog.LabelPart;
import com.nimbusphagia.library.catalog.LabelledBooks;
import com.nimbusphagia.library.exception.ErrorCodes;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.organization.AccessGuard;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Keeps catalog data that stuck labels derive from stable, and tells labels when it changes. */
@Component
@RequiredArgsConstructor
class LabelDataPolicy {
  private final ApplicationEventPublisher publisher;
  private final LabelledBooks labelledBooks;
  private final AccessGuard accessGuard;

  // Admins may change it anyway: their change recomposes the labels, which may end OUTDATED
  void requireUnlocked(Collection<Long> bookIds, Set<LabelPart> changedParts) {
    if (changedParts.isEmpty() || bookIds.isEmpty() || accessGuard.hasRole("ADMIN")) {
      return;
    }
    Set<LabelPart> locked = EnumSet.copyOf(changedParts);
    locked.retainAll(labelledBooks.derivedPartsInUse(bookIds));
    if (!locked.isEmpty()) {
      throw new ConflictException(
          ErrorCodes.LABEL_DATA_LOCKED,
          "Labels stuck on copies take " + locked + " from this data; ask an admin to change it");
    }
  }

  // Call after the change is applied: listeners read the new data in the same transaction
  void changed(Collection<Long> bookIds) {
    if (!bookIds.isEmpty()) {
      publisher.publishEvent(
          new LabelDataChanged(Set.copyOf(bookIds), accessGuard.current().getId()));
    }
  }
}
