package com.nimbusphagia.library.inventory;

import java.util.Map;
import java.util.Set;

public interface InventoryApi {

  // Damaged copies are only lent when allowDamaged is set
  void reserveForLoan(Long libraryId, Set<Long> copyIds, boolean allowDamaged);

  // Damaged returns are marked damaged, with the notes as the reason
  void releaseFromLoan(Set<Long> copyIds, boolean damaged, Long actorId, String notes);

  // Keys are copy ids, values the loan lines that lost or discarded them
  void registerLoanLoss(Long libraryId, Map<Long, Long> loanLineByCopy, Long actorId, String notes);

  void registerLoanDiscard(
      Long libraryId, Map<Long, Long> loanLineByCopy, Long actorId, String notes);
}
