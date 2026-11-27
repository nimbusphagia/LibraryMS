package com.nimbusphagia.library.catalog.internal.service;

import com.nimbusphagia.library.catalog.LabelDataChanged;
import com.nimbusphagia.library.organization.AccessGuard;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class LabelDataEvents {
  private final ApplicationEventPublisher publisher;
  private final AccessGuard accessGuard;

  // Call after the change is applied: listeners read the new data in the same transaction
  void changed(Collection<Long> bookIds) {
    if (!bookIds.isEmpty()) {
      publisher.publishEvent(
          new LabelDataChanged(Set.copyOf(bookIds), accessGuard.current().getId()));
    }
  }
}
