package com.nimbusphagia.library.organization.internal.config;

import com.nimbusphagia.library.organization.AccessGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Checks every {libraryId} and {institutionId} path variable against the current account, so a
 * new scoped endpoint is protected without remembering to call the guard.
 */
@Configuration
@RequiredArgsConstructor
class ScopeInterceptor implements HandlerInterceptor, WebMvcConfigurer {
  private final AccessGuard accessGuard;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(this).addPathPatterns("/api/**");
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    @SuppressWarnings("unchecked")
    Map<String, String> vars =
        (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    if (vars == null) {
      return true;
    }
    // Non-numeric ids are left to the controller's binding, which answers 400
    String libraryId = vars.get("libraryId");
    if (libraryId != null && libraryId.matches("\\d+")) {
      accessGuard.requireLibrary(Long.valueOf(libraryId));
    }
    String institutionId = vars.get("institutionId");
    if (institutionId != null && institutionId.matches("\\d+")) {
      accessGuard.requireInstitution(Long.valueOf(institutionId));
    }
    return true;
  }
}
