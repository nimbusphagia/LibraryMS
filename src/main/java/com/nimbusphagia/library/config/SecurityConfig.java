package com.nimbusphagia.library.config;

import com.nimbusphagia.library.exception.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  static RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy(
        """
        ROLE_OWNER > ROLE_ADMIN
        ROLE_ADMIN > ROLE_LIBRARIAN
        ROLE_LIBRARIAN > ROLE_STAFF
        """);
  }

  @Bean
  CsrfTokenRepository csrfTokenRepository() {
    return CookieCsrfTokenRepository.withHttpOnlyFalse();
  }

  @Bean
  SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfRepo) {
    return new CompositeSessionAuthenticationStrategy(
        List.of(
            new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrfRepo)));
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
    return cfg.getAuthenticationManager();
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, CsrfTokenRepository csrfRepo)
      throws Exception {
    CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
    csrfHandler.setCsrfRequestAttributeName(null);

    http.csrf(csrf -> csrf.csrfTokenRepository(csrfRepo).csrfTokenRequestHandler(csrfHandler))
        .requestCache(rc -> rc.requestCache(new NullRequestCache()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/error")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/auth/logout")
                    .logoutSuccessHandler(
                        new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (req, res, e) ->
                            writeProblem(
                                req,
                                res,
                                HttpStatus.UNAUTHORIZED,
                                ErrorCodes.UNAUTHORIZED,
                                "Authentication required"))
                    .accessDeniedHandler(
                        (req, res, e) ->
                            writeProblem(
                                req,
                                res,
                                HttpStatus.FORBIDDEN,
                                e instanceof CsrfException
                                    ? ErrorCodes.CSRF_INVALID
                                    : ErrorCodes.FORBIDDEN,
                                e instanceof CsrfException
                                    ? "Missing or invalid CSRF token"
                                    : "Access denied")))
        .addFilterAfter(new MustChangePasswordFilter(), AuthorizationFilter.class);

    return http.build();
  }

  // Security filters answer before Spring MVC, so they write the same problem shape by hand
  static void writeProblem(
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String code,
      String detail)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    String instance = request.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"");
    response
        .getWriter()
        .write(
            """
            {"type":"about:blank","title":"%s","status":%d,"detail":"%s","instance":"%s","code":"%s"}"""
                .formatted(status.getReasonPhrase(), status.value(), detail, instance, code));
  }
}
