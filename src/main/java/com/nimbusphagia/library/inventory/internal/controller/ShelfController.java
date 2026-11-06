package com.nimbusphagia.library.inventory.internal.controller;

import com.nimbusphagia.library.inventory.internal.dto.shelf.ShelfRequest;
import com.nimbusphagia.library.inventory.internal.dto.shelf.ShelfResponse;
import com.nimbusphagia.library.inventory.internal.service.ShelfService;
import com.nimbusphagia.library.shared.security.StaffOnly;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/libraries/{libraryId}/shelves")
@StaffOnly
@RequiredArgsConstructor
class ShelfController {
  private final ShelfService shelfService;

  @GetMapping
  List<ShelfResponse> findAll(@PathVariable Long libraryId) {
    return shelfService.findAll(libraryId);
  }

  @GetMapping("/{shelfId}")
  ShelfResponse findOne(@PathVariable Long libraryId, @PathVariable Long shelfId) {
    return shelfService.findOne(libraryId, shelfId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  ShelfResponse create(@PathVariable Long libraryId, @Valid @RequestBody ShelfRequest request) {
    return shelfService.create(libraryId, request);
  }

  @PutMapping("/{shelfId}")
  ShelfResponse edit(
      @PathVariable Long libraryId,
      @PathVariable Long shelfId,
      @Valid @RequestBody ShelfRequest request) {
    return shelfService.edit(libraryId, shelfId, request);
  }

  @PatchMapping("/{shelfId}/default")
  ShelfResponse makeDefault(@PathVariable Long libraryId, @PathVariable Long shelfId) {
    return shelfService.makeDefault(libraryId, shelfId);
  }

  @DeleteMapping("/{shelfId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable Long libraryId, @PathVariable Long shelfId) {
    shelfService.delete(libraryId, shelfId);
  }
}
