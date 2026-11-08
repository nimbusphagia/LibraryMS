package com.nimbusphagia.library.inventory;

import java.util.Set;

public interface InventoryApi {

  void reserveForLoan(Long libraryId, Set<Long> copyIds);

  void releaseFromLoan(Set<Long> copyIds, boolean damaged);

  void registerLoanLoss(Long libraryId, Set<Long> copyIds, Long actorId, String notes);
}
