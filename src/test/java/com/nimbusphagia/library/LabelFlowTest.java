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

  private long created(String url, String body) throws Exception {
    return read(send(post(url), as("admin"), body).andExpect(status().isCreated()), "$.id");
  }

  private long book(String json) throws Exception {
    return created("/api/books", json);
  }

  // A labelless copy of the book, so its label can be created on its own
  private long copy(long book) throws Exception {
    return read(
        send(
                post("/api/libraries/" + library + "/stock-entries"),
                as("staff"),
                "{\"reason\":\"PURCHASE\",\"items\":[{\"bookId\":%d,\"quantity\":1}]}"
                    .formatted(book))
            .andExpect(status().isCreated()),
        "$.copies[0].copyId");
  }

  private ResultActions label(long copy, String who, String parts) throws Exception {
    return send(
        post("/api/libraries/" + library + "/copies/" + copy + "/label"),
        as(who),
        "{\"parts\":" + parts + "}");
  }

  private ResultActions suggestion(long book) throws Exception {
    return mockMvc.perform(
        get("/api/libraries/" + library + "/books/" + book + "/label-suggestion")
            .with(as("staff")));
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

  // Labels

  @Test
  void nativeLabelsAreFullyDerivedAndNeedEveryPart() throws Exception {
    send(
            put("/api/libraries/" + library + "/label-scheme"),
            as("admin"),
            "{\"labelSchemeId\":" + template("Native") + "}")
        .andExpect(status().isOk());
    long narrativa = created("/api/categories", "{\"name\":\"Narrativa\",\"code\":\"NAR\"}");
    long aventura =
        created(
            "/api/categories/" + narrativa + "/subcategories",
            "{\"name\":\"Aventura\",\"code\":\"AVE\"}");
    long cervantes =
        created("/api/authors", "{\"firstName\":\"Miguel\",\"lastName\":\"Cervantes\"}");
    long quijote =
        book(
            "{\"title\":\"El Quijote\",\"authorId\":%d,\"categoryId\":%d,\"subcategoryId\":%d}"
                .formatted(cervantes, narrativa, aventura));
    long withoutSubcategory =
        book(
            "{\"title\":\"Otro\",\"authorId\":%d,\"categoryId\":%d}"
                .formatted(cervantes, narrativa));

    label(copy(withoutSubcategory), "librarian", "{}").andExpect(status().isBadRequest());
    long copy = copy(quijote);
    label(copy, "staff", "{\"TITLE\":\"XYZ\"}").andExpect(status().isBadRequest());
    label(copy, "staff", "{}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.text").value("NAR-CER-AVE-QUI-1"))
        .andExpect(jsonPath("$.parts.TITLE.origin").value("DERIVED"));
  }

  @Test
  void aCollidingLabelOffersAlternativesAndOnlyLibrariansWriteTheirOwn() throws Exception {
    long first = book("{\"title\":\"Yo, robot\"}");
    long second = book("{\"title\":\"Yo robots\"}");
    label(copy(first), "staff", "{}").andExpect(jsonPath("$.text").value("YOR / c.1"));

    suggestion(second)
        .andExpect(jsonPath("$.text").value("YOR / c.1"))
        .andExpect(jsonPath("$.collides").value(true))
        .andExpect(jsonPath("$.parts[?(@.part == 'TITLE')].alternatives[0]").value("ROB"));
    long copy = copy(second);
    label(copy, "staff", "{}").andExpect(status().isConflict());
    label(copy, "staff", "{\"TITLE\":\"MIO\"}")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("REQUIRES_LIBRARIAN"));
    label(copy, "staff", "{\"TITLE\":\"rob\"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.text").value("ROB / c.1"))
        .andExpect(jsonPath("$.parts.TITLE.origin").value("DERIVED"));
  }
}
