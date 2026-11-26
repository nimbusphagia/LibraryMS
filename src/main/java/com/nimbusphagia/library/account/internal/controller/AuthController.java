package com.nimbusphagia.library.account.internal.controller;

import com.nimbusphagia.library.account.AccountPrincipal;
import com.nimbusphagia.library.account.internal.dto.AccountMeResponse;
import com.nimbusphagia.library.account.internal.dto.AccountResponse;
import com.nimbusphagia.library.account.internal.dto.LoginRequest;
import com.nimbusphagia.library.account.internal.dto.PasswordChangeRequest;
import com.nimbusphagia.library.account.internal.dto.RegistrationRequest;
import com.nimbusphagia.library.account.internal.service.AccountService;
import com.nimbusphagia.library.exception.custom.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
class AuthController {
  private final AuthenticationManager authenticationManager;
  private final SessionAuthenticationStrategy sessionStrategy;
  private final AccountService accountService;
  private final SecurityContextRepository contextRepository =
      new HttpSessionSecurityContextRepository();
  private final SecurityContextHolderStrategy holder =
      SecurityContextHolder.getContextHolderStrategy();

  @Value("${app.registration.open:false}")
  private boolean registrationOpen;

  @PostMapping("/login")
  AccountMeResponse login(
      @Valid @RequestBody LoginRequest body, HttpServletRequest req, HttpServletResponse res) {
    Authentication auth =
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));

    sessionStrategy.onAuthentication(auth, req, res);
    saveContext(auth, req, res);

    return AccountMeResponse.from((AccountPrincipal) auth.getPrincipal());
  }

  @GetMapping("/me")
  AccountMeResponse me(@AuthenticationPrincipal AccountPrincipal principal) {
    return AccountMeResponse.from(principal);
  }

  @PostMapping("/password")
  AccountMeResponse changePassword(
      @Valid @RequestBody PasswordChangeRequest body,
      @AuthenticationPrincipal AccountPrincipal principal,
      HttpServletRequest req,
      HttpServletResponse res) {
    accountService.changeOwnPassword(principal.getId(), body.currentPassword(), body.newPassword());

    // The session holds a copy of the principal; refresh it so the password-change gate lifts
    AccountPrincipal updated = principal.withPasswordChanged();
    saveContext(
        UsernamePasswordAuthenticationToken.authenticated(updated, null, updated.getAuthorities()),
        req,
        res);
    return AccountMeResponse.from(updated);
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  AccountResponse register(@Valid @RequestBody RegistrationRequest body) {
    if (!registrationOpen) {
      throw new ResourceNotFoundException("Resource not found");
    }
    return accountService.register(body);
  }

  // Helpers
  private void saveContext(Authentication auth, HttpServletRequest req, HttpServletResponse res) {
    SecurityContext ctx = holder.createEmptyContext();
    ctx.setAuthentication(auth);
    holder.setContext(ctx);
    contextRepository.saveContext(ctx, req, res);
  }
}
