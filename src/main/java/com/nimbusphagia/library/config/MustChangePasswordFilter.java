package com.nimbusphagia.library.config;

import com.nimbusphagia.library.account.AccountPrincipal;
import com.nimbusphagia.library.exception.ErrorCodes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Until a seeded or reset password is replaced, the account can only see itself and change it. */
class MustChangePasswordFilter extends OncePerRequestFilter {
  private static final Set<String> ALLOWED =
      Set.of("GET /api/auth/me", "POST /api/auth/password", "POST /api/auth/logout");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null
        && auth.getPrincipal() instanceof AccountPrincipal principal
        && principal.isMustChangePassword()
        && !ALLOWED.contains(request.getMethod() + " " + request.getRequestURI())) {
      SecurityConfig.writeProblem(
          request,
          response,
          HttpStatus.FORBIDDEN,
          ErrorCodes.PASSWORD_CHANGE_REQUIRED,
          "You must change your password before continuing");
      return;
    }
    chain.doFilter(request, response);
  }
}
