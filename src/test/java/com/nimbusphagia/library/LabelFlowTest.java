package com.nimbusphagia.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import java.util.Arrays;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
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

  // A copy registered without label parts
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

  private ResultActions create(long book, String who, String parts) throws Exception {
    return send(
        post("/api/libraries/" + library + "/books/" + book + "/label"),
        as(who),
        "{\"parts\":" + parts + "}");
  }

  private ResultActions pattern(long book) throws Exception {
    return mockMvc.perform(
        get("/api/libraries/" + library + "/books/" + book + "/label").with(as("staff")));
  }

  private ResultActions copyLabel(long copy) throws Exception {
    return mockMvc.perform(
        get("/api/libraries/" + library + "/copies/" + copy + "/label").with(as("staff")));
  }

  private ResultActions pending() throws Exception {
    return mockMvc.perform(
        get("/api/libraries/" + library + "/labels/pending").with(as("staff")));
  }

  private ResultActions markPlaced(long copy, String text) throws Exception {
    return send(
        post("/api/libraries/" + library + "/labels/mark-placed"),
        as("staff"),
        "{\"labels\":[{\"copyId\":%d,\"text\":\"%s\"}]}".formatted(copy, text));
  }

  private ResultActions refresh(long book, String parts) throws Exception {
    return send(
        post("/api/libraries/" + library + "/books/" + book + "/label/refresh"),
        as("staff"),
        parts == null ? "" : "{\"parts\":" + parts + "}");
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

    create(withoutSubcategory, "librarian", "{}").andExpect(status().isBadRequest());
    long copy = copy(quijote);
    copyLabel(copy).andExpect(jsonPath("$.state").value("NO_PATTERN"));
    create(quijote, "staff", "{\"TITLE\":\"XYZ\"}").andExpect(status().isBadRequest());
    create(quijote, "staff", "{}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("NAR-CER-AVE-QUI"))
        .andExpect(jsonPath("$.parts.TITLE.origin").value("DERIVED"));
    copyLabel(copy)
        .andExpect(jsonPath("$.state").value("UNLABELLED"))
        .andExpect(jsonPath("$.text").value("NAR-CER-AVE-QUI-1"));

    // Later copies follow the book's label and cannot depart from it
    copyLabel(copy(quijote)).andExpect(jsonPath("$.text").value("NAR-CER-AVE-QUI-2"));
    create(quijote, "librarian", "{}").andExpect(status().isConflict());
    send(
            post("/api/libraries/" + library + "/stock-entries"),
            as("librarian"),
            """
            {"reason":"PURCHASE","items":[{"bookId":%d,"quantity":1,
             "labelParts":{"AUTHOR_LETTERS":"CERV"}}]}"""
                .formatted(quijote))
        .andExpect(status().isBadRequest());
  }

  @Test
  void aCollidingLabelOffersAlternativesAndOnlyLibrariansWriteTheirOwn() throws Exception {
    long first = book("{\"title\":\"Yo, robot\"}");
    long second = book("{\"title\":\"Yo robots\"}");
    create(first, "staff", "{}").andExpect(jsonPath("$.code").value("YOR"));

    suggestion(second)
        .andExpect(jsonPath("$.text").value("YOR / c.1"))
        .andExpect(jsonPath("$.collides").value(true))
        .andExpect(jsonPath("$.parts[?(@.part == 'TITLE')].alternatives[0]").value("ROB"));
    create(second, "staff", "{}").andExpect(status().isConflict());
    create(second, "staff", "{\"TITLE\":\"MIO\"}")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("REQUIRES_LIBRARIAN"));
    create(second, "staff", "{\"TITLE\":\"rob\"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("ROB"))
        .andExpect(jsonPath("$.parts.TITLE.origin").value("DERIVED"));
  }

  @Test
  void aLabelStillStuckOnACopyIsNotFreeForAnotherBook() throws Exception {
    long first = book("{\"title\":\"Yo, robot\"}");
    long stuck = copy(first);
    create(first, "staff", "{}");
    placeLabel(library, stuck);
    send(
            put("/api/libraries/" + library + "/books/" + first + "/label"),
            as("librarian"),
            "{\"parts\":{\"TITLE\":\"ASI\"},\"changeReason\":\"Shorter\"}")
        .andExpect(status().isOk());

    long second = book("{\"title\":\"Yo robots\"}");
    copy(second);
    create(second, "staff", "{}").andExpect(status().isConflict());

    placeLabel(library, stuck);
    create(second, "staff", "{}").andExpect(jsonPath("$.code").value("YOR"));
  }

  @Test
  void malagaSuggestsTheAuthorsCodesThenTheFloorAndPrefixEntries() throws Exception {
    jdbc.update(
        """
        INSERT INTO malaga_entry (letter, entry, qualifier, number) VALUES
          ('C', 'Cer', NULL, 45), ('C', 'Cera', NULL, 46),
          ('C', 'Cera', NULL, 48), ('C', 'Ces', NULL, 49)""");
    long cervantes =
        created("/api/authors", "{\"firstName\":\"Miguel\",\"lastName\":\"Cervantes\"}");
    String book =
        "{\"title\":\"%s\",\"authorId\":" + cervantes + ",\"deweyNumber\":\"%s\"}";
    String malaga = "{\"AUTHOR_MALAGA\":\"%s\"}";
    create(book(book.formatted("Galatea", "860")), "librarian", malaga.formatted("C9"))
        .andExpect(status().isCreated());
    create(book(book.formatted("Novelas", "863.3")), "librarian", malaga.formatted("C45"))
        .andExpect(status().isCreated());

    // "cera" is the greatest entry not after "cervantes"; "cer" is its longest prefix
    suggestion(book(book.formatted("Quijote", "863.3")))
        .andExpect(
            jsonPath("$.parts[?(@.part == 'AUTHOR_MALAGA')].malaga[*].code")
                .value(contains("C45", "C9", "C46", "C4", "C48", "C45", "C4")));
  }

  // Recomposition

  @Test
  void aPatternChangeOutdatesOnlyPlacedLabelsAndUndoingItSettlesThem() throws Exception {
    long novela = created("/api/categories", "{\"name\":\"Novela\",\"code\":\"NOV\"}");
    long quijote = book("{\"title\":\"Quijote\",\"categoryId\":" + novela + "}");
    long placed = copy(quijote);
    long unlabelled = copy(quijote);
    create(quijote, "staff", "{}");
    placeLabel(library, placed);

    send(
            put("/api/libraries/" + library + "/label-scheme").param("dryRun", "true"),
            as("admin"),
            "{\"labelSchemeId\":" + template("Native") + "}")
        .andExpect(jsonPath("$.unresolved").value(1));
    pattern(quijote).andExpect(jsonPath("$.schemeId").value(template("Spring")));

    String category = "{\"name\":\"Novela\",\"code\":\"%s\"}";
    send(put("/api/categories/" + novela), as("admin"), category.formatted("NAR"))
        .andExpect(status().isOk());

    pattern(quijote).andExpect(jsonPath("$.code").value("NAR-QUI"));
    copyLabel(placed)
        .andExpect(jsonPath("$.state").value("OUTDATED"))
        .andExpect(jsonPath("$.placedText").value("NOV-QUI / c.1"))
        .andExpect(jsonPath("$.text").value("NAR-QUI / c.1"));
    copyLabel(unlabelled).andExpect(jsonPath("$.text").value("NAR-QUI / c.2"));
    pending().andExpect(jsonPath("$[*].copyId", contains((int) placed, (int) unlabelled)));
    refresh(quijote, null).andExpect(status().isBadRequest());

    send(put("/api/categories/" + novela), as("admin"), category.formatted("NOV"))
        .andExpect(status().isOk());
    copyLabel(placed).andExpect(jsonPath("$.state").value("LABELLED"));
    // Printed before the change was undone
    markPlaced(unlabelled, "NAR-QUI / c.2").andExpect(status().isConflict());
  }

  @Test
  void aPatternThatWouldCollideWaitsOutdatedForAnAlternative() throws Exception {
    long quijote = book("{\"title\":\"Quijote\"}");
    long other = book("{\"title\":\"Hamlet\"}");
    create(quijote, "staff", "{}");
    long copy = copy(other);
    create(other, "staff", "{}");

    // Its new title would give it the code of the Quijote, so nothing changes yet
    send(put("/api/books/" + other), as("admin"), "{\"title\":\"Quijote 2\"}")
        .andExpect(status().isOk());
    pattern(other).andExpect(jsonPath("$.outdated").value(true));
    copyLabel(copy).andExpect(jsonPath("$.text").value("HAM / c.1"));
    pending().andExpect(jsonPath("$").isEmpty());
    markPlaced(copy, "HAM / c.1").andExpect(status().isBadRequest());

    refresh(other, null)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.alternatives.TITLE[0]").value("2"));
    refresh(other, "{\"TITLE\":\"2\"}").andExpect(jsonPath("$.outdated").value(false));
    copyLabel(copy).andExpect(jsonPath("$.text").value("2 / c.1"));
  }

  // Sheet

  @Test
  void aSheetPrintsOneLayoutLinePerRowInTheOrderAsked() throws Exception {
    long novela = created("/api/categories", "{\"name\":\"Novela\",\"code\":\"NOV\"}");
    long quijote = book("{\"title\":\"Quijote\",\"categoryId\":" + novela + "}");
    long first = copy(quijote);
    long second = copy(quijote);
    create(quijote, "staff", "{}");
    // Outside Latin-1, which the PDF font covers
    long polish = created("/api/categories", "{\"name\":\"Polaca\",\"code\":\"ŁOD\"}");
    long lalka = book("{\"title\":\"Lalka\",\"categoryId\":" + polish + "}");
    long polishCopy = copy(lalka);
    create(lalka, "staff", "{}");

    MvcResult result =
        sheet(second, first, polishCopy)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn();
    try (PDDocument pdf = Loader.loadPDF(result.getResponse().getContentAsByteArray())) {
      String text = new PDFTextStripper().getText(pdf);
      assertThat(text)
          .containsSubsequence(
              "#" + second, "NOV-QUI", "c.2", "#" + first, "c.1", "#" + polishCopy)
          .contains("?OD-LAL")
          .doesNotContain(" / ");
    }

    long unlabelled = copy(book("{\"title\":\"Hamlet\"}"));
    sheet(first, unlabelled).andExpect(status().isBadRequest());
    sheet(first, first).andExpect(status().isBadRequest());
  }

  private ResultActions sheet(long... copies) throws Exception {
    return send(
        post("/api/libraries/" + library + "/labels/sheet"),
        as("staff"),
        "{\"copyIds\":" + Arrays.toString(copies) + "}");
  }

  @Test
  void librariansAreLockedOnlyByPlacedLabelsThatDeriveTheData() throws Exception {
    // Librarians write only the exclusive catalog
    String own = "/api/institutions/" + institutionId;
    long novela = created(own + "/categories", "{\"name\":\"Novela\",\"code\":\"NOV\"}");
    String quijote = "{\"title\":\"%s\",\"categoryId\":" + novela + "}";
    long book = created(own + "/books", quijote.formatted("Quijote"));
    long copy = copy(book);
    create(book, "librarian", "{\"TITLE\":\"CER\"}").andExpect(status().isCreated());

    // Nothing stuck yet: nothing is locked
    send(
            put(own + "/categories/" + novela),
            as("librarian"),
            "{\"name\":\"Novela\",\"code\":\"NV\"}")
        .andExpect(status().isOk());
    placeLabel(library, copy);

    // The title is MANUAL, but the category code is DERIVED
    send(put(own + "/books/" + book), as("librarian"), quijote.formatted("Don Quijote"))
        .andExpect(status().isOk());
    send(
            put(own + "/categories/" + novela),
            as("librarian"),
            "{\"name\":\"Novela\",\"code\":\"NAR\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("LABEL_DATA_LOCKED"));
  }
}
