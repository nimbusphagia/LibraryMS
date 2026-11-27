package com.nimbusphagia.library.inventory.internal.controller;

import com.nimbusphagia.library.inventory.internal.dto.scheme.LabelSchemeRequest;
import com.nimbusphagia.library.inventory.internal.dto.scheme.LabelSchemeResponse;
import com.nimbusphagia.library.inventory.internal.service.LabelSchemeService;
import com.nimbusphagia.library.shared.security.AdminOnly;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/label-schemes")
@AdminOnly
@RequiredArgsConstructor
class LabelSchemeController {
  private final LabelSchemeService schemeService;

  @GetMapping
  List<LabelSchemeResponse> findAll() {
    return schemeService.findAll();
  }

  @GetMapping("/{id}")
  LabelSchemeResponse findOne(@PathVariable Long id) {
    return schemeService.findOne(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  LabelSchemeResponse create(@Valid @RequestBody LabelSchemeRequest request) {
    return schemeService.create(request);
  }

  // A dry run counts the patterns that would change, the stuck labels left outdated and the
  // patterns that could not follow, and saves nothing
  @PutMapping("/{id}")
  ResponseEntity<?> edit(
      @PathVariable Long id,
      @RequestParam(defaultValue = "false") boolean dryRun,
      @Valid @RequestBody LabelSchemeRequest request) {
    return ResponseEntity.ok(
        dryRun ? schemeService.preview(id, request) : schemeService.edit(id, request));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable Long id) {
    schemeService.delete(id);
  }
}
