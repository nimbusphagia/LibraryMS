package com.nimbusphagia.library.inventory.internal.controller;

import com.nimbusphagia.library.inventory.internal.dto.library.LibraryRequest;
import com.nimbusphagia.library.inventory.internal.dto.library.LibraryResponse;
import com.nimbusphagia.library.inventory.internal.dto.scheme.LabelSchemeAssignRequest;
import com.nimbusphagia.library.inventory.internal.service.LibraryService;
import com.nimbusphagia.library.shared.security.AdminOnly;
import com.nimbusphagia.library.shared.security.StaffOnly;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
class LibraryController {
  private final LibraryService libraryService;

  @GetMapping("/libraries")
  @StaffOnly
  List<LibraryResponse> findAll(
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) Long institutionId) {
    return libraryService.findAll(active, institutionId);
  }

  @GetMapping("/libraries/{libraryId}")
  @StaffOnly
  LibraryResponse findOne(@PathVariable Long libraryId) {
    return libraryService.findOne(libraryId);
  }

  @PostMapping("/institutions/{institutionId}/libraries")
  @AdminOnly
  @ResponseStatus(HttpStatus.CREATED)
  LibraryResponse create(
      @PathVariable Long institutionId, @Valid @RequestBody LibraryRequest request) {
    return libraryService.create(institutionId, request);
  }

  @PutMapping("/libraries/{libraryId}")
  @AdminOnly
  LibraryResponse edit(@PathVariable Long libraryId, @Valid @RequestBody LibraryRequest request) {
    return libraryService.edit(libraryId, request);
  }

  // A dry run counts the patterns that would change, the stuck labels left outdated and the
  // patterns that could not follow, and saves nothing
  @PutMapping("/libraries/{libraryId}/label-scheme")
  @AdminOnly
  ResponseEntity<?> assignScheme(
      @PathVariable Long libraryId,
      @RequestParam(defaultValue = "false") boolean dryRun,
      @Valid @RequestBody LabelSchemeAssignRequest request) {
    Long schemeId = request.labelSchemeId();
    return ResponseEntity.ok(
        dryRun
            ? libraryService.previewScheme(libraryId, schemeId)
            : libraryService.assignScheme(libraryId, schemeId));
  }

  @PatchMapping("/libraries/{libraryId}/deactivate")
  @AdminOnly
  LibraryResponse deactivate(@PathVariable Long libraryId) {
    return libraryService.deactivate(libraryId);
  }

  @PatchMapping("/libraries/{libraryId}/activate")
  @AdminOnly
  LibraryResponse activate(@PathVariable Long libraryId) {
    return libraryService.activate(libraryId);
  }
}
