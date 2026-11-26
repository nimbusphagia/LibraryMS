package com.nimbusphagia.library.exception.custom;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {
  public ConflictException(String message) {
    this("CONFLICT", message);
  }

  public ConflictException(String code, String message) {
    super(HttpStatus.CONFLICT, code, message);
  }
}
