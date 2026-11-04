package com.nimbusphagia.library.exception.custom;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {
  public UnauthorizedException(String message) {
    super(HttpStatus.UNAUTHORIZED, message);
  }
}
