package com.nimbusphagia.library_ms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class SmokeTest extends IntegrationTestBase {

  @Test
  void authorsListIsPublic() throws Exception {
    mockMvc
        .perform(get("/api/authors"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());
  }

  @Test
  void booksRequireSession() throws Exception {
    mockMvc.perform(get("/api/books")).andExpect(status().isUnauthorized());
  }
}
