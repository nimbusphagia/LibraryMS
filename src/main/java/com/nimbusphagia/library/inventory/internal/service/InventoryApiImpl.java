package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.exception.ErrorCodes;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ForbiddenException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.InventoryApi;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.inventory.internal.entity.CopyStatus;
import com.nimbusphagia.library.inventory.internal.entity.Library;
import com.nimbusphagia.library.inventory.internal.entity.MovementReason;
import com.nimbusphagia.library.inventory.internal.repository.CopyRepository;
import com.nimbusphagia.library.inventory.internal.repository.LibraryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryApiImpl implements InventoryApi {
  private final LibraryRepository libraryRepository;
  private final CopyRepository copyRepository;
  private final StockMovementService stockMovementService;
  private final CopyService copyService;

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void reserveForLoan(Long libraryId, Set<Long> copyIds, boolean allowDamaged) {
    if (copyIds.isEmpty()) {
      return;
    }
    Library library =
        libraryRepository
            .findById(libraryId)
            .orElseThrow(() -> new ResourceNotFoundException("Library not found"));
    if (!library.isActive()) {
      throw new BadRequestException("This library is inactive");
    }

    Map<Long, Copy> byId = lock(copyIds);
    List<Copy> copies = new ArrayList<>();
    for (Long id : copyIds) {
      Copy copy = byId.get(id);
      if (copy == null || !copy.getLibrary().getId().equals(libraryId)) {
        throw new ResourceNotFoundException("Copy " + id + " not found in this library");
      }
      if (copy.getStatus() != CopyStatus.AVAILABLE) {
        throw new BadRequestException(
            "Copy " + id + " is " + copy.getStatus() + " and cannot be loaned");
      }
      if (copy.isDamaged() && !allowDamaged) {
        throw new ForbiddenException(
            ErrorCodes.REQUIRES_LIBRARIAN, "Copy " + id + " is damaged; ask a librarian to lend it");
      }
      copies.add(copy);
    }

    for (Copy copy : copies) {
      copy.setStatus(CopyStatus.ON_LOAN);
    }
    copyRepository.saveAllAndFlush(copies);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void releaseFromLoan(Set<Long> copyIds, boolean damaged, Long actorId, String notes) {
    if (copyIds.isEmpty()) {
      return;
    }

    Map<Long, Copy> byId = lock(copyIds);
    List<Copy> copies = new ArrayList<>();
    for (Long id : copyIds) {
      Copy copy = byId.get(id);
      if (copy == null) {
        throw new ResourceNotFoundException("Copy " + id + " not found");
      }
      if (copy.getStatus() != CopyStatus.ON_LOAN) {
        throw new BadRequestException(
            "Copy " + id + " is " + copy.getStatus() + " and is not on loan");
      }
      copies.add(copy);
    }

    for (Copy copy : copies) {
      copy.setStatus(CopyStatus.AVAILABLE);
    }
    copyRepository.saveAllAndFlush(copies);
    if (damaged) {
      for (Copy copy : copies) {
        if (!copy.isDamaged()) {
          copyService.markDamaged(copy, actorId, notes);
        }
      }
    }
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void registerLoanLoss(
      Long libraryId, Map<Long, Long> loanLineByCopy, Long actorId, String notes) {
    if (!loanLineByCopy.isEmpty()) {
      stockMovementService.registerLoanExit(
          libraryId, MovementReason.LOSS, loanLineByCopy, notes, actorId);
    }
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void registerLoanDiscard(
      Long libraryId, Map<Long, Long> loanLineByCopy, Long actorId, String notes) {
    if (!loanLineByCopy.isEmpty()) {
      stockMovementService.registerLoanExit(
          libraryId, MovementReason.DAMAGE, loanLineByCopy, notes, actorId);
    }
  }

  // Helpers
  private Map<Long, Copy> lock(Set<Long> copyIds) {
    Map<Long, Copy> byId = new HashMap<>();
    for (Copy copy : copyRepository.findAllByIdInForUpdate(copyIds)) {
      byId.put(copy.getId(), copy);
    }
    return byId;
  }
}
