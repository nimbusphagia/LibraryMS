package com.nimbusphagia.library_ms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nimbusphagia.library_ms.account.internal.entity.AccountRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class AuthFlowTest extends IntegrationTestBase {

  private String anonymousCsrfToken() throws Exception {
    MvcResult result =
        mockMvc.perform(get("/api/auth/me")).andExpect(cookie().exists("XSRF-TOKEN")).andReturn();
    return result.getResponse().getCookie("XSRF-TOKEN").getValue();
  }

  private ResultActions login(String username, String password) throws Exception {
    String token = anonymousCsrfToken();
    return mockMvc.perform(
        post("/api/auth/login")
            .cookie(new Cookie("XSRF-TOKEN", token))
            .header("X-XSRF-TOKEN", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)));
  }

  @Test
  void anonymousRequestReceivesCsrfCookie() throws Exception {
    mockMvc
        .perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(cookie().exists("XSRF-TOKEN"));
  }

  @Test
  void loginSucceedsAndSetsCookies() throws Exception {
    login("admin", PASSWORD)
        .andExpect(status().isOk())
        // HttpOnly/SameSite no se verifican aqui: MockMvc no pasa por Tomcat y no refleja
        // los atributos que la app real envia
        .andExpect(cookie().exists("SESSION"))
        .andExpect(jsonPath("$.username").value("admin"))
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @ParameterizedTest
  @CsvSource({"admin,wrong-password", "ghost,whatever", "inactive-user,Test-pass-123"})
  void loginFailsWith401(String username, String password) throws Exception {
    createAccount("inactive-user", AccountRole.STAFF, false);

    login(username, password).andExpect(status().isUnauthorized());
  }

  @Test
  void loginWithoutCsrfIsForbidden() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"" + PASSWORD + "\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void usernameIsCaseInsensitive() throws Exception {
    login("ADMIN", PASSWORD)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("admin"));
  }
}
