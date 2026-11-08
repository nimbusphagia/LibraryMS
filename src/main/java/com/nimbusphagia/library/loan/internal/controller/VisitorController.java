package com.nimbusphagia.library.loan.internal.controller;

import com.nimbusphagia.library.loan.internal.dto.visitor.VisitorFilter;
import com.nimbusphagia.library.loan.internal.dto.visitor.VisitorRequest;
import com.nimbusphagia.library.loan.internal.dto.visitor.VisitorResponse;
import com.nimbusphagia.library.loan.internal.service.VisitorService;
import com.nimbusphagia.library.shared.security.StaffOnly;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/visitors")
@StaffOnly
@RequiredArgsConstructor
class VisitorController {
  private final VisitorService visitorService;

  @GetMapping
  Page<VisitorResponse> findAll(
      VisitorFilter filter, @PageableDefault(size = 20, sort = "name") Pageable pageable) {
    return visitorService.findAll(filter, pageable);
  }

  @GetMapping("/{id}")
  VisitorResponse findOne(@PathVariable Long id) {
    return visitorService.findOne(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  VisitorResponse create(@Valid @RequestBody VisitorRequest request) {
    return visitorService.create(request);
  }

  @PutMapping("/{id}")
  VisitorResponse edit(@PathVariable Long id, @Valid @RequestBody VisitorRequest request) {
    return visitorService.edit(id, request);
  }

  @PatchMapping("/{id}/deactivate")
  VisitorResponse deactivate(@PathVariable Long id) {
    return visitorService.deactivate(id);
  }

  @PatchMapping("/{id}/activate")
  VisitorResponse activate(@PathVariable Long id) {
    return visitorService.activate(id);
  }
}
