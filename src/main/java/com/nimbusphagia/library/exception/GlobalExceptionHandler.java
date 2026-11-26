package com.nimbusphagia.library.exception;

import com.nimbusphagia.library.exception.custom.ApiException;
import com.nimbusphagia.library.shared.security.AdminOnly;
import com.nimbusphagia.library.shared.security.LibrarianOnly;
import com.nimbusphagia.library.shared.security.OwnerOnly;
import com.nimbusphagia.library.shared.security.StaffOnly;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every error leaves as an RFC 9457 problem ({@code application/problem+json}) carrying a {@code
 * code} the front can switch on. Spring MVC's own exceptions are handled by the base class.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
    return problem(ex.getStatus(), ex.getCode(), ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
    return problem(HttpStatus.CONFLICT, ErrorCodes.CONFLICT, "Data integrity violation");
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex) {
    return problem(
        HttpStatus.CONFLICT,
        ErrorCodes.CONCURRENT_MODIFICATION,
        "The resource was modified by someone else. Reload and try again");
  }

  @ExceptionHandler(PropertyReferenceException.class)
  ResponseEntity<ProblemDetail> handleBadSort(PropertyReferenceException ex) {
    return problem(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, "Invalid sort property");
  }

  // Method security denials name the role the endpoint requires, so the front can say who to ask.
  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ProblemDetail> handleAccessDenied(
      AccessDeniedException ex, HandlerMethod handler) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth instanceof AnonymousAuthenticationToken) {
      throw ex;
    }
    return problem(
        HttpStatus.FORBIDDEN,
        requiredRoleCode(handler),
        "You do not have permission to perform this action");
  }

  @ExceptionHandler(AuthenticationException.class)
  void rethrowAuthentication(AuthenticationException ex) {
    throw ex;
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
    log.error("Unexpected error on {}", request.getRequestURI(), ex);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "Internal server error");
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    Map<String, String> errors = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));

    ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "Validation failed");
    body.setProperty("code", ErrorCodes.VALIDATION_FAILED);
    body.setProperty("errors", errors);
    return handleExceptionInternal(ex, body, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    ResponseEntity<Object> response =
        super.handleExceptionInternal(ex, body, headers, statusCode, request);
    if (response != null && response.getBody() instanceof ProblemDetail problem) {
      Map<String, Object> props = problem.getProperties();
      if (props == null || !props.containsKey("code")) {
        problem.setProperty("code", ErrorCodes.forStatus(statusCode.value()));
      }
    }
    return response;
  }

  // Helpers
  private static ResponseEntity<ProblemDetail> problem(
      HttpStatus status, String code, String detail) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
    body.setProperty("code", code);
    return ResponseEntity.status(status).body(body);
  }

  private static String requiredRoleCode(HandlerMethod handler) {
    String code = roleCodeOf(handler.getMethod());
    if (code == null) {
      code = roleCodeOf(handler.getBeanType());
    }
    return code == null ? ErrorCodes.FORBIDDEN : code;
  }

  private static String roleCodeOf(AnnotatedElement element) {
    if (AnnotatedElementUtils.hasAnnotation(element, OwnerOnly.class)) {
      return ErrorCodes.REQUIRES_OWNER;
    }
    if (AnnotatedElementUtils.hasAnnotation(element, AdminOnly.class)) {
      return ErrorCodes.REQUIRES_ADMIN;
    }
    if (AnnotatedElementUtils.hasAnnotation(element, LibrarianOnly.class)) {
      return ErrorCodes.REQUIRES_LIBRARIAN;
    }
    if (AnnotatedElementUtils.hasAnnotation(element, StaffOnly.class)) {
      return ErrorCodes.REQUIRES_STAFF;
    }
    return null;
  }
}
