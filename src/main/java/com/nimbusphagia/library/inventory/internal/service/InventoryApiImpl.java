package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.InventoryApi;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.inventory.internal.entity.CopyCondition;
import com.nimbusphagia.library.inventory.internal.entity.CopyStatus;
import com.nimbusphagia.library.inventory.internal.entity.Library;
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

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void reserveForLoan(Long libraryId, Set<Long> copyIds) {
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
      copies.add(copy);
    }

    for (Copy copy : copies) {
      copy.setStatus(CopyStatus.ON_LOAN);
    }
    copyRepository.saveAllAndFlush(copies);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void releaseFromLoan(Set<Long> copyIds, boolean damaged) {
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
      if (damaged) {
        copy.setCondition(CopyCondition.DAMAGED);
      }
    }
    copyRepository.saveAllAndFlush(copies);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void registerLoanLoss(Long libraryId, Set<Long> copyIds, Long actorId, String notes) {
    if (copyIds.isEmpty()) {
      return;
    }
    stockMovementService.registerLoanLoss(libraryId, List.copyOf(copyIds), notes, actorId);
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
