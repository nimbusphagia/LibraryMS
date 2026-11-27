package com.nimbusphagia.library.exception.custom;

import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ApiException extends RuntimeException {
  private final HttpStatus status;
  private final String code;
  // Extra members of the problem body, for data the client acts on
  private final Map<String, Object> properties;

  protected ApiException(HttpStatus status, String code, String message) {
    this(status, code, message, Map.of());
  }

  protected ApiException(
      HttpStatus status, String code, String message, Map<String, Object> properties) {
    super(message);
    this.status = status;
    this.code = code;
    this.properties = properties;
  }
}
