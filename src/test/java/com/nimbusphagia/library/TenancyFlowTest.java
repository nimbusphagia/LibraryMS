package com.nimbusphagia.library;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.account.internal.entity.AccountRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class TenancyFlowTest extends IntegrationTestBase {

  private ResultActions doGet(String url, RequestPostProcessor who) throws Exception {
    return mockMvc.perform(get(url).with(who));
  }

  private ResultActions doPost(String url, RequestPostProcessor who, String body) throws Exception {
    return mockMvc.perform(
        post(url).with(who).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private long library(Long institution, String name) throws Exception {
    String body =
        doPost(
                "/api/institutions/" + institution + "/libraries",
                as("admin"),
                "{\"name\":\"" + name + "\"}")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }

  // Isolation

  @Test
  void clientAccountsOnlyReachLibrariesOfTheirInstitution() throws Exception {
    long own = library(institutionId, "Main");
    long other = library(createInstitution(organizationId, "Other school"), "Main");

    doGet("/api/libraries/" + own + "/shelves", as("staff")).andExpect(status().isOk());
    doGet("/api/libraries/" + other + "/shelves", as("staff"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    doGet("/api/libraries/" + other + "/shelves", as("librarian")).andExpect(status().isNotFound());
    doGet("/api/libraries/" + other + "/shelves", as("admin")).andExpect(status().isOk());
  }

  @Test
  void organizationsAreIsolatedFromEachOther() throws Exception {
    long ownLibrary = library(institutionId, "Main");
    Long foreignOrg =
        jdbc.queryForObject(
            "INSERT INTO organization (name) VALUES ('Foreign') RETURNING id", Long.class);
    Long foreignInstitution = createInstitution(foreignOrg, "Foreign school");
    Long foreignLibrary =
        jdbc.queryForObject(
            "INSERT INTO library (institution_id, name) VALUES (?, 'Foreign') RETURNING id",
            Long.class,
            foreignInstitution);

    doGet("/api/libraries/" + foreignLibrary, as("owner")).andExpect(status().isNotFound());
    doGet("/api/institutions/" + foreignInstitution, as("admin")).andExpect(status().isNotFound());
    doGet("/api/libraries", as("owner"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(ownLibrary));
  }

  @Test
  void libraryListOfAClientAccountIgnoresOtherInstitutions() throws Exception {
    library(institutionId, "Main");
    long other = createInstitution(organizationId, "Other school");
    library(other, "Annex");

    doGet("/api/libraries?institutionId=" + other, as("staff"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Main"));
  }

  // Roles

  @Test
  void staffCannotRegisterStockExitsAndIsToldWhoCan() throws Exception {
    long lib = library(institutionId, "Main");

    doPost(
            "/api/libraries/" + lib + "/stock-exits",
            as("staff"),
            "{\"reason\":\"DISCARD\",\"copyIds\":[1]}")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("REQUIRES_LIBRARIAN"));
  }

  @Test
  void onlyAdminsCreateLibraries() throws Exception {
    doPost("/api/institutions/" + institutionId + "/libraries", as("librarian"), "{\"name\":\"X\"}")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("REQUIRES_ADMIN"));
  }

  // Accounts

  @Test
  void librarianCreatesStaffOfTheirInstitutionOnly() throws Exception {
    String staff =
        """
        {"username":"new-staff","fullName":"New","password":"%s","role":"STAFF","institutionId":%d}""";
    doPost("/api/accounts", as("librarian"), staff.formatted(PASSWORD, institutionId))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.mustChangePassword").value(true));

    String librarian =
        """
        {"username":"new-lib","fullName":"New","password":"%s","role":"LIBRARIAN","institutionId":%d}""";
    doPost("/api/accounts", as("librarian"), librarian.formatted(PASSWORD, institutionId))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("REQUIRES_ADMIN"));

    long other = createInstitution(organizationId, "Other school");
    doPost(
            "/api/accounts",
            as("librarian"),
            staff.formatted(PASSWORD, other).replace("new-staff", "x"))
        .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @ValueSource(strings = {"STAFF", "LIBRARIAN"})
  void clientRolesRequireAnInstitution(String role) throws Exception {
    doPost(
            "/api/accounts",
            as("admin"),
            "{\"username\":\"x\",\"fullName\":\"X\",\"password\":\"%s\",\"role\":\"%s\"}"
                .formatted(PASSWORD, role))
        .andExpect(status().isBadRequest());
  }

  @Test
  void accountsWithAPendingPasswordChangeCanOnlyChangeIt() throws Exception {
    Account account = createAccount("fresh", AccountRole.STAFF, true);
    account.setMustChangePassword(true);
    accounts.save(account);

    doGet("/api/libraries", as("fresh"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
    doGet("/api/auth/me", as("fresh")).andExpect(status().isOk());
    doPost(
            "/api/auth/password",
            as("fresh"),
            "{\"currentPassword\":\"%s\",\"newPassword\":\"Another-pass-456\"}".formatted(PASSWORD))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mustChangePassword").value(false));
    doGet("/api/libraries", as("fresh")).andExpect(status().isOk());
  }

  @Test
  void registrationIsClosedByDefault() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"organizationName":"Demo","username":"demo","fullName":"Demo","password":"%s"}"""
                        .formatted(PASSWORD)))
        .andExpect(status().isNotFound());
  }
}
