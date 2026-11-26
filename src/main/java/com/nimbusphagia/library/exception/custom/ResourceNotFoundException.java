package com.nimbusphagia.library.exception.custom;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {
  public ResourceNotFoundException(String message) {
    this("NOT_FOUND", message);
  }

  public ResourceNotFoundException(String code, String message) {
    super(HttpStatus.NOT_FOUND, code, message);
  }
}
