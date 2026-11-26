package com.nimbusphagia.library;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import com.nimbusphagia.library.account.internal.entity.AccountRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class CatalogScopeFlowTest extends IntegrationTestBase {

  private Long otherInstitution;

  @BeforeEach
  void createOtherInstitution() {
    otherInstitution = createInstitution(organizationId, "Other school");
    createAccount("other-librarian", AccountRole.LIBRARIAN, true, organizationId, otherInstitution);
  }

  private ResultActions doGet(String url, RequestPostProcessor who) throws Exception {
    return mockMvc.perform(get(url).with(who));
  }

  private ResultActions doPost(String url, RequestPostProcessor who, String body) throws Exception {
    return mockMvc.perform(
        post(url).with(who).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private ResultActions doPut(String url, RequestPostProcessor who, String body) throws Exception {
    return mockMvc.perform(
        put(url).with(who).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private long created(ResultActions result) throws Exception {
    String body =
        result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private String own(String path) {
    return "/api/institutions/" + institutionId + path;
  }

  private String other(String path) {
    return "/api/institutions/" + otherInstitution + path;
  }

  @Test
  void exclusiveCatalogIsWrittenByStaffAndHiddenFromOtherInstitutions() throws Exception {
    created(
        doPost("/api/authors", as("admin"), "{\"firstName\":\"Gabriel\",\"lastName\":\"García\"}"));
    doPost("/api/authors", as("librarian"), "{\"firstName\":\"Isabel\"}")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("REQUIRES_ADMIN"));

    long allende =
        created(
            doPost(
                    own("/authors"),
                    as("staff"),
                    "{\"firstName\":\"Isabel\",\"lastName\":\"Allende\"}")
                .andExpect(jsonPath("$.institutionId").value(institutionId)));
    // An exclusive row may not repeat a shared one
    doPost(own("/authors"), as("staff"), "{\"firstName\":\"GABRIEL\",\"lastName\":\"garcía\"}")
        .andExpect(status().isConflict());

    doGet("/api/authors", as("staff")).andExpect(jsonPath("$.content", hasSize(1)));
    doGet(own("/authors?scope=EXCLUSIVE"), as("staff"))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(allende));
    doGet(own("/authors"), as("staff")).andExpect(jsonPath("$.content", hasSize(2)));
    doGet(own("/authors?search=garcia"), as("staff")).andExpect(jsonPath("$.content", hasSize(1)));

    doGet(own("/authors"), as("other-librarian")).andExpect(status().isNotFound());
    doGet(other("/authors"), as("other-librarian")).andExpect(jsonPath("$.content", hasSize(1)));

    String byAllende = "{\"title\":\"Paula\",\"authorId\":" + allende + "}";
    long paula = created(doPost(own("/books"), as("staff"), byAllende));
    doPost(other("/books"), as("other-librarian"), byAllende).andExpect(status().isNotFound());
    doPost("/api/books", as("admin"), byAllende).andExpect(status().isNotFound());

    doGet(own("/books/" + paula), as("staff")).andExpect(status().isOk());
    doGet(other("/books/" + paula), as("other-librarian")).andExpect(status().isNotFound());
    doGet("/api/books/" + paula, as("staff")).andExpect(status().isNotFound());
  }

  @Test
  void otherOrganizationsSeeNoneOfTheCatalog() throws Exception {
    long book =
        created(
            doPost("/api/books", as("admin"), "{\"title\":\"Libro\",\"isbn\":\"9780306406157\"}"));
    Long foreignOrg =
        jdbc.queryForObject(
            "INSERT INTO organization (name) VALUES ('Foreign') RETURNING id", Long.class);
    createAccount("foreign-admin", AccountRole.ADMIN, true, foreignOrg, null);

    doGet("/api/books/" + book, as("foreign-admin")).andExpect(status().isNotFound());
    doGet("/api/books", as("foreign-admin")).andExpect(jsonPath("$.content", hasSize(0)));
    created(
        doPost(
            "/api/books", as("foreign-admin"), "{\"title\":\"Otro\",\"isbn\":\"9780306406157\"}"));
  }

  @Test
  void labelDataOfAnExclusiveBookLocksOnceLabelled() throws Exception {
    long library = created(doPost(own("/libraries"), as("admin"), "{\"name\":\"Main\"}"));
    long category =
        created(doPost(own("/categories"), as("staff"), "{\"name\":\"Cuentos\",\"code\":\"CU\"}"));
    long book =
        created(
            doPost(
                own("/books"),
                as("staff"),
                "{\"title\":\"Cuentos\",\"categoryId\":%d}".formatted(category)));
    long foreignBook =
        created(doPost(other("/books"), as("other-librarian"), "{\"title\":\"Ajeno\"}"));

    String entry =
        """
        {"reason":"PURCHASE","items":[{"bookId":%d,"quantity":1,
         "labelParts":{"titleCode":"CUE"}}]}""";
    String entries = "/api/libraries/" + library + "/stock-entries";
    doPost(entries, as("staff"), entry.formatted(foreignBook)).andExpect(status().isNotFound());
    doPost(entries, as("staff"), entry.formatted(book)).andExpect(status().isCreated());

    String retitled = "{\"title\":\"Otro titulo\",\"categoryId\":%d}".formatted(category);
    doPut(own("/books/" + book), as("librarian"), retitled)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("LABEL_DATA_LOCKED"));
    doPut(
            own("/books/" + book),
            as("staff"),
            "{\"title\":\"Cuentos\",\"categoryId\":%d,\"description\":\"Nueva\"}"
                .formatted(category))
        .andExpect(status().isOk());

    doPut(own("/categories/" + category), as("staff"), "{\"name\":\"Cuentos\",\"code\":\"CC\"}")
        .andExpect(jsonPath("$.code").value("LABEL_DATA_LOCKED"));
    doPut(own("/categories/" + category), as("staff"), "{\"name\":\"Relatos\",\"code\":\"CU\"}")
        .andExpect(status().isOk());

    doPut(own("/books/" + book), as("admin"), retitled).andExpect(status().isOk());
  }
}
