package com.nimbusphagia.library.inventory;

import java.util.Set;

public interface InventoryApi {

  // Damaged copies are only lent when allowDamaged is set
  void reserveForLoan(Long libraryId, Set<Long> copyIds, boolean allowDamaged);

  void releaseFromLoan(Set<Long> copyIds, boolean damaged);

  void registerLoanLoss(Long libraryId, Set<Long> copyIds, Long actorId, String notes);
}
