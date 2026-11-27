package com.nimbusphagia.library;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class LabelFlowTest extends IntegrationTestBase {

  private static final String MINIMAL_LAYOUT =
      """
      {"lines":[{"separator":"-","parts":[
        {"part":"TITLE","required":true,"editable":true},
        {"part":"COPY_NUMBER","required":true,"editable":false}]}]}""";

  private long library;

  @BeforeEach
  void createLibrary() throws Exception {
    library =
        read(
            send(
                    post("/api/institutions/" + institutionId + "/libraries"),
                    as("admin"),
                    "{\"name\":\"Main\"}")
                .andExpect(status().isCreated()),
            "$.id");
  }

  private ResultActions send(
      MockHttpServletRequestBuilder request, RequestPostProcessor who, String body)
      throws Exception {
    return mockMvc.perform(
        request.with(who).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private long read(ResultActions result, String path) throws Exception {
    String body = result.andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(body, path)).longValue();
  }

  private long template(String name) {
    return jdbc.queryForObject(
        "SELECT id FROM label_scheme WHERE organization_id IS NULL AND name = ?", Long.class, name);
  }

  private ResultActions createScheme(String name, String layout) throws Exception {
    return send(
        post("/api/label-schemes"),
        as("admin"),
        "{\"name\":\"" + name + "\",\"layout\":" + layout + "}");
  }

  // Schemes

  @Test
  void librariesStartOnSpringAndAdminsAssignACopiedScheme() throws Exception {
    mockMvc
        .perform(get("/api/libraries/" + library).with(as("staff")))
        .andExpect(jsonPath("$.labelSchemeId").value(template("Spring")));
    mockMvc
        .perform(get("/api/label-schemes").with(as("admin")))
        .andExpect(jsonPath("$[*].name", contains("Native", "Spring")));

    send(put("/api/label-schemes/" + template("Native")), as("admin"), "{\"name\":\"X\"}")
        .andExpect(status().isBadRequest());
    send(
            put("/api/label-schemes/" + template("Native")),
            as("admin"),
            "{\"name\":\"X\",\"layout\":" + MINIMAL_LAYOUT + "}")
        .andExpect(status().isForbidden());

    long scheme =
        read(createScheme("Mine", MINIMAL_LAYOUT).andExpect(status().isCreated()), "$.id");
    send(
            put("/api/libraries/" + library + "/label-scheme"),
            as("librarian"),
            "{\"labelSchemeId\":" + scheme + "}")
        .andExpect(status().isForbidden());
    send(
            put("/api/libraries/" + library + "/label-scheme"),
            as("admin"),
            "{\"labelSchemeId\":" + scheme + "}")
        .andExpect(jsonPath("$.labelSchemeId").value(scheme));

    send(delete("/api/label-schemes/" + scheme), as("admin"), "")
        .andExpect(status().isConflict());
  }

  @Test
  void aLayoutNeedsAFixedCopyNumberAndAnotherRequiredPart() throws Exception {
    for (String layout :
        new String[] {
          // no copy number
          """
          {"lines":[{"parts":[{"part":"TITLE","required":true,"editable":true}]}]}""",
          // editable copy number
          """
          {"lines":[{"parts":[{"part":"TITLE","required":true,"editable":true},
            {"part":"COPY_NUMBER","required":true,"editable":true}]}]}""",
          // nothing else required
          """
          {"lines":[{"parts":[{"part":"TITLE","required":false,"editable":true},
            {"part":"COPY_NUMBER","required":true,"editable":false}]}]}""",
          // repeated part
          """
          {"lines":[{"parts":[{"part":"TITLE","required":true,"editable":true}]},
            {"parts":[{"part":"TITLE","required":true,"editable":true},
            {"part":"COPY_NUMBER","required":true,"editable":false}]}]}""",
          // fixed Malaga code
          """
          {"lines":[{"parts":[{"part":"AUTHOR_MALAGA","required":true,"editable":false},
            {"part":"COPY_NUMBER","required":true,"editable":false}]}]}"""
        }) {
      createScheme("Bad", layout).andExpect(status().isBadRequest());
    }
  }
}
