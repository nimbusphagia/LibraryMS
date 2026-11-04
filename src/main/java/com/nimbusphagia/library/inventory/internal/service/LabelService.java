package com.nimbusphagia.library.inventory.internal.service;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.catalog.BookLabelData;
import com.nimbusphagia.library.catalog.CatalogApi;
import com.nimbusphagia.library.exception.custom.BadRequestException;
import com.nimbusphagia.library.exception.custom.ConflictException;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelCustomizeRequest;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelDraft;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelResponse;
import com.nimbusphagia.library.inventory.internal.dto.label.LabelRevisionResponse;
import com.nimbusphagia.library.inventory.internal.entity.Copy;
import com.nimbusphagia.library.inventory.internal.entity.Label;
import com.nimbusphagia.library.inventory.internal.entity.LabelRevision;
import com.nimbusphagia.library.inventory.internal.entity.PrintStatus;
import com.nimbusphagia.library.inventory.internal.repository.LabelRepository;
import com.nimbusphagia.library.inventory.internal.repository.LabelRevisionRepository;
import com.nimbusphagia.library.inventory.internal.util.MalagaResolver;
import com.nimbusphagia.library.inventory.internal.util.MalagaResult;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabelService {
  private static final String INITIAL_REASON = "Initial generation";

  private final LabelRepository labelRepository;
  private final LabelRevisionRepository revisionRepository;
  private final CatalogApi catalogApi;
  private final MalagaResolver malagaResolver;
  private final LabelAssembler assembler;
  private final EntityManager entityManager;

  @Transactional(propagation = Propagation.MANDATORY)
  public LabelResponse generateFor(Copy copy, Long actorId) {
    if (labelRepository.existsByCopyId(copy.getId())) {
      throw new ConflictException("This copy already has a label");
    }
    BookLabelData book = catalogApi.getBookLabelData(copy.getBook().getId());
    MalagaResult malaga = malagaResolver.resolve(copy.getLibrary().getId(), book);
    LabelDraft draft = assembler.assemble(book, copy.getIteration(), malaga);

    Label label =
        Label.builder()
            .library(copy.getLibrary())
            .book(copy.getBook())
            .copy(copy)
            .categoryCode(draft.categoryCode())
            .collectionCode(draft.collectionCode())
            .deweyCode(draft.deweyCode())
            .malagaSource(draft.malagaSource())
            .malagaCode(draft.malagaCode())
            .collectionVolume(draft.collectionVolume())
            .seriesVolume(draft.seriesVolume())
            .copyNumber(draft.copyNumber())
            .text(draft.text())
            .build();

    saveAndFlush(label);
    writeRevision(label, actorId, INITIAL_REASON);
    return LabelResponse.fromEntity(label);
  }

  @Transactional
  public LabelResponse customize(Long copyId, LabelCustomizeRequest request, Long actorId) {
    Label label = findActiveByCopy(copyId);
    if (request.text().equals(label.getText())) {
      throw new BadRequestException("The new text is the same as the current one");
    }
    label.setText(request.text());
    label.setCustomized(true);
    label.setRevision(label.getRevision() + 1);
    resetPrint(label);

    saveAndFlush(label);
    writeRevision(label, actorId, request.changeReason());
    return LabelResponse.fromEntity(label);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void retire(Long copyId) {
    labelRepository
        .findByCopyId(copyId)
        .filter(label -> label.getRetiredAt() == null)
        .ifPresent(
            label -> {
              label.setRetiredAt(Instant.now());
              labelRepository.saveAndFlush(label);
            });
  }

  @Transactional
  public List<LabelResponse> markPrinted(Set<Long> labelIds, Long actorId) {
    List<Label> labels = labelRepository.findByIdIn(labelIds);
    if (labels.size() != labelIds.size()) {
      throw new ResourceNotFoundException("Some labels were not found");
    }
    if (labels.stream().anyMatch(l -> l.getRetiredAt() != null)) {
      throw new BadRequestException("Retired labels cannot be printed");
    }
    Account actor = actorRef(actorId);
    Instant now = Instant.now();
    for (Label label : labels) {
      if (label.getPrintStatus() == PrintStatus.PRINTED) continue;
      label.setPrintStatus(PrintStatus.PRINTED);
      label.setPrintedAt(now);
      label.setPrintedBy(actor);
    }
    labelRepository.saveAll(labels);
    return labels.stream()
        .sorted(Comparator.comparing(Label::getId))
        .map(LabelResponse::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<LabelResponse> findPendingPrint(Long libraryId) {
    return labelRepository
        .findActiveByLibraryAndPrintStatus(libraryId, PrintStatus.PENDING_PRINT)
        .stream()
        .map(LabelResponse::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public LabelResponse getByCopyId(Long copyId) {
    return LabelResponse.fromEntity(findByCopy(copyId));
  }

  @Transactional(readOnly = true)
  public List<LabelRevisionResponse> getHistory(Long copyId) {
    Label label = findByCopy(copyId);
    return revisionRepository.findByLabelIdOrderByRevisionNumberDesc(label.getId()).stream()
        .map(LabelRevisionResponse::fromEntity)
        .toList();
  }

  // Helpers

  private Label findByCopy(Long copyId) {
    return labelRepository
        .findByCopyId(copyId)
        .orElseThrow(() -> new ResourceNotFoundException("Label not found for this copy"));
  }

  private Label findActiveByCopy(Long copyId) {
    Label label = findByCopy(copyId);
    if (label.getRetiredAt() != null) {
      throw new BadRequestException("This label is retired and cannot be modified");
    }
    return label;
  }

  private static void resetPrint(Label label) {
    label.setPrintStatus(PrintStatus.PENDING_PRINT);
    label.setPrintedAt(null);
    label.setPrintedBy(null);
  }

  private void writeRevision(Label label, Long actorId, String reason) {
    revisionRepository.save(
        LabelRevision.builder()
            .label(label)
            .revisionNumber(label.getRevision())
            .categoryCode(label.getCategoryCode())
            .collectionCode(label.getCollectionCode())
            .deweyCode(label.getDeweyCode())
            .malagaSource(label.getMalagaSource())
            .malagaCode(label.getMalagaCode())
            .collectionVolume(label.getCollectionVolume())
            .seriesVolume(label.getSeriesVolume())
            .copyNumber(label.getCopyNumber())
            .text(label.getText())
            .changedBy(actorRef(actorId))
            .changeReason(reason)
            .build());
  }

  private Account actorRef(Long accountId) {
    return entityManager.getReference(Account.class, accountId);
  }

  private void saveAndFlush(Label label) {
    try {
      labelRepository.saveAndFlush(label);
    } catch (DataIntegrityViolationException e) {
      throw translate(e);
    }
  }

  private RuntimeException translate(DataIntegrityViolationException e) {
    String cause = String.valueOf(NestedExceptionUtils.getMostSpecificCause(e).getMessage());
    if (cause.contains("uq_label_library_text_lower")) {
      return new ConflictException("Another active label in this library already has this text");
    }
    if (cause.contains("uq_label_library_book_copy_number")) {
      return new ConflictException("This copy number is already in use for this book");
    }
    if (cause.contains("uq_label_copy")) {
      return new ConflictException("This copy already has a label");
    }
    return e;
  }
}
