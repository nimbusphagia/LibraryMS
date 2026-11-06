package com.nimbusphagia.library.inventory.internal.controller;

import com.nimbusphagia.library.inventory.internal.dto.library.LibraryRequest;
import com.nimbusphagia.library.inventory.internal.dto.library.LibraryResponse;
import com.nimbusphagia.library.inventory.internal.service.LibraryService;
import com.nimbusphagia.library.shared.security.AdminOnly;
import com.nimbusphagia.library.shared.security.StaffOnly;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/libraries")
@RequiredArgsConstructor
class LibraryController {
  private final LibraryService libraryService;

  @GetMapping
  @StaffOnly
  List<LibraryResponse> findAll(@RequestParam(required = false) Boolean active) {
    return libraryService.findAll(active);
  }

  @GetMapping("/{id}")
  @StaffOnly
  LibraryResponse findOne(@PathVariable Long id) {
    return libraryService.findOne(id);
  }

  @PostMapping
  @AdminOnly
  @ResponseStatus(HttpStatus.CREATED)
  LibraryResponse create(@Valid @RequestBody LibraryRequest request) {
    return libraryService.create(request);
  }

  @PutMapping("/{id}")
  @AdminOnly
  LibraryResponse edit(@PathVariable Long id, @Valid @RequestBody LibraryRequest request) {
    return libraryService.edit(id, request);
  }

  @PatchMapping("/{id}/deactivate")
  @AdminOnly
  LibraryResponse deactivate(@PathVariable Long id) {
    return libraryService.deactivate(id);
  }

  @PatchMapping("/{id}/activate")
  @AdminOnly
  LibraryResponse activate(@PathVariable Long id) {
    return libraryService.activate(id);
  }
}
