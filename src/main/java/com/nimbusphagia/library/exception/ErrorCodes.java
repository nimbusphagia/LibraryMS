package com.nimbusphagia.library.exception;

public final class ErrorCodes {
  public static final String BAD_REQUEST = "BAD_REQUEST";
  public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
  public static final String UNAUTHORIZED = "UNAUTHORIZED";
  public static final String FORBIDDEN = "FORBIDDEN";
  public static final String NOT_FOUND = "NOT_FOUND";
  public static final String CONFLICT = "CONFLICT";
  public static final String CONCURRENT_MODIFICATION = "CONCURRENT_MODIFICATION";
  public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

  public static final String CSRF_INVALID = "CSRF_INVALID";
  public static final String PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED";
  public static final String REQUIRES_STAFF = "REQUIRES_STAFF";
  public static final String REQUIRES_LIBRARIAN = "REQUIRES_LIBRARIAN";
  public static final String REQUIRES_ADMIN = "REQUIRES_ADMIN";
  public static final String REQUIRES_OWNER = "REQUIRES_OWNER";

  public static final String OVERDUE_LOANS = "OVERDUE_LOANS";
  public static final String VISITOR_HAS_OPEN_LOANS = "VISITOR_HAS_OPEN_LOANS";

  public static final String LABEL_DATA_LOCKED = "LABEL_DATA_LOCKED";

  private ErrorCodes() {}

  public static String forStatus(int status) {
    return switch (status) {
      case 400 -> BAD_REQUEST;
      case 401 -> UNAUTHORIZED;
      case 403 -> FORBIDDEN;
      case 404 -> NOT_FOUND;
      case 409 -> CONFLICT;
      case 500 -> INTERNAL_ERROR;
      default -> "HTTP_" + status;
    };
  }
}
