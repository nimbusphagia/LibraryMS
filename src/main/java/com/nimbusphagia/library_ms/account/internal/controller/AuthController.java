package com.nimbusphagia.library_ms.account.internal.controller;

import com.nimbusphagia.library_ms.account.AccountPrincipal;
import com.nimbusphagia.library_ms.account.internal.dto.AccountMeResponse;
import com.nimbusphagia.library_ms.account.internal.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
class AuthController {
  private final AuthenticationManager authenticationManager;
  private final SessionAuthenticationStrategy sessionStrategy;
  private final SecurityContextRepository contextRepository =
      new HttpSessionSecurityContextRepository();
  private final SecurityContextHolderStrategy holder =
      SecurityContextHolder.getContextHolderStrategy();

  @PostMapping("/login")
  AccountMeResponse login(
      @Valid @RequestBody LoginRequest body, HttpServletRequest req, HttpServletResponse res) {
    Authentication auth =
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));

    sessionStrategy.onAuthentication(auth, req, res);

    SecurityContext ctx = holder.createEmptyContext();
    ctx.setAuthentication(auth);
    holder.setContext(ctx);
    contextRepository.saveContext(ctx, req, res);

    return AccountMeResponse.from((AccountPrincipal) auth.getPrincipal());
  }

  @GetMapping("/me")
  AccountMeResponse me(@AuthenticationPrincipal AccountPrincipal principal) {
    return AccountMeResponse.from(principal);
  }
}
