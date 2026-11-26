package com.nimbusphagia.library;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class VisitorFlowTest extends IntegrationTestBase {

  private static final String ROLE =
      """
      {"name":"%s","requiresGrade":false,"maxDays":7,"maxActiveLoans":5,
       "overdueBlockThreshold":1,"graceDays":0}""";

  private ResultActions doPost(String url, RequestPostProcessor who, String body) throws Exception {
    return mockMvc.perform(
        post(url).with(who).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private long created(ResultActions result) throws Exception {
    String body =
        result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  private long role(Long institution) throws Exception {
    return created(
        doPost(
            "/api/institutions/" + institution + "/visitor-roles",
            as("admin"),
            ROLE.formatted("Reader")));
  }

  private long visitor(Long institution, long role) throws Exception {
    return created(
        doPost(
            "/api/institutions/" + institution + "/visitors",
            as("staff"),
            "{\"name\":\"Ana\",\"roleId\":" + role + "}"));
  }

  private long library(Long institution) throws Exception {
    return library(institution, "Main");
  }

  private long library(Long institution, String name) throws Exception {
    return created(
        doPost(
            "/api/institutions/" + institution + "/libraries",
            as("admin"),
            "{\"name\":\"" + name + "\"}"));
  }

  private long copy(long library) {
    Long book =
        jdbc.queryForObject("INSERT INTO book (title) VALUES ('Book') RETURNING id", Long.class);
    return jdbc.queryForObject(
        """
        INSERT INTO copy (library_id, book_id, shelf_id, iteration)
        SELECT ?, ?, id, 1 FROM shelf WHERE library_id = ? AND is_default
        RETURNING id""",
        Long.class,
        library,
        book,
        library);
  }

  private ResultActions lend(long library, long visitor, long copy, String who) throws Exception {
    return doPost(
        "/api/libraries/" + library + "/loans",
        as(who),
        "{\"visitorId\":" + visitor + ",\"copyIds\":[" + copy + "]}");
  }

  @Test
  void institutionsStartWithTheTemplatesAndOnlyLibrariansAdjustThem() throws Exception {
    doPost("/api/templates/visitor-roles", as("librarian"), ROLE.formatted("Reader"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("REQUIRES_ADMIN"));
    created(doPost("/api/templates/visitor-roles", as("admin"), ROLE.formatted("Reader")));
    created(
        doPost(
            "/api/templates/school-grades", as("admin"), "{\"level\":\"PRIMARY\",\"grade\":1}"));

    long school =
        created(
            doPost(
                "/api/institutions",
                as("admin"),
                "{\"name\":\"New school\",\"timeZone\":\"America/Lima\"}"));
    mockMvc
        .perform(get("/api/institutions/" + school + "/visitor-roles").with(as("admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Reader"));
    mockMvc
        .perform(get("/api/institutions/" + school + "/school-grades").with(as("admin")))
        .andExpect(jsonPath("$.length()").value(1));

    String ownRoles = "/api/institutions/" + institutionId + "/visitor-roles";
    doPost(ownRoles, as("staff"), ROLE.formatted("Guest"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("REQUIRES_LIBRARIAN"));
    created(doPost(ownRoles, as("librarian"), ROLE.formatted("Guest")));
  }

  @Test
  void visitorsStayInsideTheirInstitution() throws Exception {
    long role = role(institutionId);
    long visitor = visitor(institutionId, role);
    long other = createInstitution(organizationId, "Other school");

    mockMvc
        .perform(get("/api/institutions/" + other + "/visitors").with(as("staff")))
        .andExpect(status().isNotFound());
    doPost(
            "/api/institutions/" + other + "/visitors",
            as("admin"),
            "{\"name\":\"Bea\",\"roleId\":" + role + "}")
        .andExpect(status().isNotFound());

    long otherLibrary = library(other);
    lend(otherLibrary, visitor, copy(otherLibrary), "admin")
        .andExpect(status().isNotFound());
  }

  @Test
  void overdueLoansBlockNewOnesAndOpenLoansBlockDeactivation() throws Exception {
    long visitor = visitor(institutionId, role(institutionId));
    long library = library(institutionId);
    lend(library, visitor, copy(library), "staff").andExpect(status().isCreated());

    mockMvc
        .perform(
            patch("/api/institutions/" + institutionId + "/visitors/" + visitor + "/deactivate")
                .with(as("staff"))
                .with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("VISITOR_HAS_OPEN_LOANS"));

    jdbc.update(
        "UPDATE loan_movement SET created_at = now() - interval '30 days',"
            + " expires_by = now() - interval '1 day'");
    lend(library, visitor, copy(library), "staff")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("OVERDUE_LOANS"));

    long annex = library(institutionId, "Annex");
    lend(annex, visitor, copy(annex), "staff").andExpect(status().isCreated());
  }

  @Test
  void onlyLibrariansLendDamagedCopies() throws Exception {
    long visitor = visitor(institutionId, role(institutionId));
    long library = library(institutionId);
    long damaged = copy(library);
    doPost(
            "/api/libraries/" + library + "/copies/" + damaged + "/damage",
            as("librarian"),
            "{\"reason\":\"Torn pages\"}")
        .andExpect(status().isOk());

    lend(library, visitor, damaged, "staff")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("REQUIRES_LIBRARIAN"));
    lend(library, visitor, damaged, "librarian").andExpect(status().isCreated());
  }
}
