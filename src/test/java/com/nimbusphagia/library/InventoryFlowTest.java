package com.nimbusphagia.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class InventoryFlowTest extends IntegrationTestBase {

  private long library;
  private Long visitor;

  @BeforeEach
  void createLibrary() throws Exception {
    library = library(institutionId, "Main");
  }

  private ResultActions doPost(String url, RequestPostProcessor who, String body) throws Exception {
    return mockMvc.perform(
        post(url).with(who).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private long read(ResultActions result, String path) throws Exception {
    String body = result.andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(body, path)).longValue();
  }

  private long library(Long institution, String name) throws Exception {
    return read(
        doPost(
                "/api/institutions/" + institution + "/libraries",
                as("admin"),
                "{\"name\":\"" + name + "\"}")
            .andExpect(status().isCreated()),
        "$.id");
  }

  private long book() {
    return jdbc.queryForObject("INSERT INTO book (title) VALUES ('Book') RETURNING id", Long.class);
  }

  // Registers a labeled copy through a stock entry
  private long copy(long book, String titleCode) throws Exception {
    return read(
        doPost(
                "/api/libraries/" + library + "/stock-entries",
                as("staff"),
                """
                {"reason":"PURCHASE","items":[{"bookId":%d,"quantity":1,
                 "labelParts":{"titleCode":"%s"}}]}"""
                    .formatted(book, titleCode))
            .andExpect(status().isCreated()),
        "$.copies[0].copyId");
  }

  private long copy(long book) throws Exception {
    return copy(book, "T" + book);
  }

  private long copy() throws Exception {
    return copy(book());
  }

  private long visitor() throws Exception {
    if (visitor != null) {
      return visitor;
    }
    long role =
        read(
            doPost(
                "/api/institutions/" + institutionId + "/visitor-roles",
                as("admin"),
                """
                {"name":"Reader","requiresGrade":false,"maxDays":7,"maxActiveLoans":5,
                 "overdueBlockThreshold":1,"graceDays":0}"""),
            "$.id");
    visitor =
        read(
            doPost(
                "/api/institutions/" + institutionId + "/visitors",
                as("staff"),
                "{\"name\":\"Ana\",\"roleId\":" + role + "}"),
            "$.id");
    return visitor;
  }

  private ResultActions lend(long copy) throws Exception {
    return doPost(
        "/api/libraries/" + library + "/loans",
        as("staff"),
        "{\"visitorId\":" + visitor() + ",\"copyIds\":[" + copy + "]}");
  }

  // Lends the copy and resolves its loan line with the outcome
  private ResultActions resolution(long copy, String outcome, String who) throws Exception {
    ResultActions loan = lend(copy).andExpect(status().isCreated());
    return doPost(
        "/api/libraries/" + library + "/loans/" + read(loan, "$.id") + "/resolutions",
        as(who),
        "{\"lines\":[{\"lineId\":%d,\"outcome\":\"%s\"}]}"
            .formatted(read(loan, "$.lines[0].id"), outcome));
  }

  private long resolve(long copy, String outcome) throws Exception {
    resolution(copy, outcome, "librarian").andExpect(status().isOk());
    return jdbc.queryForObject(
        "SELECT id FROM loan_movement_line WHERE copy_id = ?", Long.class, copy);
  }

  private ResultActions exit(long copy, String reason) throws Exception {
    return doPost(
        "/api/libraries/" + library + "/stock-exits",
        as("librarian"),
        "{\"reason\":\"" + reason + "\",\"copyIds\":[" + copy + "]}");
  }

  private ResultActions recover(long copy, String who) throws Exception {
    return doPost(
        "/api/libraries/" + library + "/stock-recoveries", as(who), "{\"copyIds\":[" + copy + "]}");
  }

  private ResultActions transfer(long copy, long destination, String who) throws Exception {
    return doPost(
        "/api/libraries/" + library + "/stock-transfers",
        as(who),
        "{\"destinationLibraryId\":" + destination + ",\"copyIds\":[" + copy + "]}");
  }

  private ResultActions condition(long copy, String action, String who) throws Exception {
    return doPost(
        "/api/libraries/" + library + "/copies/" + copy + "/" + action,
        as(who),
        "{\"reason\":\"Torn pages\"}");
  }

  private Map<String, Object> copyRow(long copy) {
    return jdbc.queryForMap("SELECT * FROM copy WHERE id = ?", copy);
  }

  private Map<String, Object> labelRow(long copy) {
    return jdbc.queryForMap("SELECT * FROM label WHERE copy_id = ?", copy);
  }

  // Permissions

  @ParameterizedTest
  @CsvSource({
    "staff,damage,403",
    "librarian,damage,200",
    "staff,repair,403",
    "librarian,repair,200",
    "staff,exit,403",
    "librarian,exit,201",
    "staff,transfer,403",
    "librarian,transfer,201",
    "staff,LOST,403",
    "librarian,LOST,200",
    "staff,DISCARDED,403",
    "librarian,DISCARDED,200",
    "staff,RETURNED_DAMAGED,200",
    "staff,entry,201",
    "staff,recovery,201",
  })
  void exitsAndDamageNeedALibrarianWhileEntriesDoNot(String who, String action, int expected)
      throws Exception {
    long copy = copy();
    ResultActions result =
        switch (action) {
          case "damage" -> condition(copy, "damage", who);
          case "repair" -> {
            condition(copy, "damage", "librarian").andExpect(status().isOk());
            yield condition(copy, "repair", who);
          }
          case "exit" ->
              doPost(
                  "/api/libraries/" + library + "/stock-exits",
                  as(who),
                  "{\"reason\":\"DISCARD\",\"copyIds\":[" + copy + "]}");
          case "transfer" -> transfer(copy, library(institutionId, "Annex"), who);
          case "entry" ->
              doPost(
                  "/api/libraries/" + library + "/stock-entries",
                  as(who),
                  "{\"reason\":\"DONATION\",\"items\":[{\"bookId\":%d,\"quantity\":1}]}"
                      .formatted(book()));
          case "recovery" -> {
            resolve(copy, "LOST");
            yield recover(copy, who);
          }
          default -> resolution(copy, action, who);
        };
    result.andExpect(status().is(expected));
    if (expected == 403) {
      result.andExpect(jsonPath("$.code").value("REQUIRES_LIBRARIAN"));
    }
  }

  // Damage

  @Test
  void damageIsRecordedAndRepairLetsStaffLendTheCopyAgain() throws Exception {
    long copy = copy();
    condition(copy, "damage", "librarian").andExpect(status().isOk());
    condition(copy, "damage", "librarian").andExpect(status().isConflict());
    lend(copy)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("REQUIRES_LIBRARIAN"));

    condition(copy, "repair", "librarian").andExpect(status().isOk());
    assertThat(copyRow(copy).get("damaged_at")).isNull();
    mockMvc
        .perform(
            get("/api/libraries/" + library + "/copies/" + copy + "/condition-events")
                .with(as("staff")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].type").value(contains("REPAIRED", "DAMAGED")));
    lend(copy).andExpect(status().isCreated());
  }

  @Test
  void aCopyReturnedDamagedIsMarkedDamaged() throws Exception {
    long copy = copy();
    resolution(copy, "RETURNED_DAMAGED", "staff").andExpect(status().isOk());

    assertThat(copyRow(copy)).containsEntry("status", "AVAILABLE");
    assertThat(copyRow(copy).get("damaged_at")).isNotNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM copy_condition_event WHERE copy_id = ? AND type = 'DAMAGED'",
                Integer.class,
                copy))
        .isEqualTo(1);
  }

  // Exits

  @ParameterizedTest
  @CsvSource({"LOST,LOSS,LOST", "DISCARDED,DAMAGE,DISCARDED"})
  void lostAndDiscardedLoansLeaveAnExitLinkedToTheLoanLine(
      String outcome, String reason, String copyStatus) throws Exception {
    long copy = copy();
    long line = resolve(copy, outcome);

    assertThat(copyRow(copy)).containsEntry("status", copyStatus);
    assertThat(labelRow(copy).get("retired_at")).isNotNull();
    assertThat(
            jdbc.queryForMap(
                """
                SELECT m.type, m.reason, l.copy_id FROM stock_movement_line l
                JOIN stock_movement m ON m.id = l.movement_id
                WHERE l.loan_line_id = ?""",
                line))
        .containsEntry("type", "EXIT")
        .containsEntry("reason", reason)
        .containsEntry("copy_id", copy);
  }

  @Test
  void transfersAndRecoveriesHaveTheirOwnOperations() throws Exception {
    exit(copy(), "TRANSFER").andExpect(status().isBadRequest());
    for (String reason : new String[] {"TRANSFER", "RECOVERY"}) {
      doPost(
              "/api/libraries/" + library + "/stock-entries",
              as("librarian"),
              "{\"reason\":\"%s\",\"items\":[{\"bookId\":%d,\"quantity\":1}]}"
                  .formatted(reason, book()))
          .andExpect(status().isBadRequest());
    }
  }

  // Recovery

  @Test
  void aRecoveredCopyKeepsItsNumberAndLabel() throws Exception {
    long copy = copy();
    Map<String, Object> label = labelRow(copy);
    long lossLine =
        jdbc.queryForObject(
            "SELECT id FROM stock_movement_line WHERE loan_line_id = ?",
            Long.class,
            resolve(copy, "LOST"));

    recover(copy, "staff").andExpect(status().isCreated());

    assertThat(copyRow(copy)).containsEntry("status", "AVAILABLE").containsEntry("iteration", 1);
    assertThat(labelRow(copy))
        .containsEntry("id", label.get("id"))
        .containsEntry("text", label.get("text"))
        .containsEntry("retired_at", null);
    assertThat(
            jdbc.queryForMap(
                """
                SELECT m.type, m.reason, l.copy_id FROM stock_movement_line l
                JOIN stock_movement m ON m.id = l.movement_id
                WHERE l.recovered_line_id = ?""",
                lossLine))
        .containsEntry("type", "ENTRY")
        .containsEntry("reason", "RECOVERY")
        .containsEntry("copy_id", copy);
  }

  @Test
  void aRecoveredCopyWhoseNumberWasTakenGetsTheNextOneAndANewLabel() throws Exception {
    long book = book();
    long copy = copy(book);
    Object oldText = labelRow(copy).get("text");
    resolve(copy, "LOST");
    long replacement = copy(book);
    assertThat(copyRow(replacement)).containsEntry("iteration", 1);

    recover(copy, "staff").andExpect(status().isCreated());

    assertThat(copyRow(copy)).containsEntry("iteration", 2);
    assertThat(labelRow(copy))
        .containsEntry("retired_at", null)
        .containsEntry("print_status", "PENDING_PRINT")
        .doesNotContainEntry("text", oldText);
  }

  @Test
  void aRecoveredLabelWhoseTextIsTakenComesBackOutdated() throws Exception {
    long book = book();
    long copy = copy(book);
    resolve(copy, "LOST");
    copy(book(), "T" + book);

    recover(copy, "staff").andExpect(status().isCreated());

    assertThat(copyRow(copy)).containsEntry("iteration", 1);
    assertThat(labelRow(copy))
        .containsEntry("retired_at", null)
        .containsEntry("print_status", "OUTDATED");
  }

  @Test
  void onlyLostCopiesAreRecovered() throws Exception {
    long discarded = copy();
    resolve(discarded, "DISCARDED");
    long lostThenDiscarded = copy();
    resolve(lostThenDiscarded, "LOST");
    exit(lostThenDiscarded, "DISCARD").andExpect(status().isCreated());

    recover(copy(), "staff").andExpect(status().isBadRequest());
    recover(discarded, "staff").andExpect(status().isBadRequest());
    recover(lostThenDiscarded, "staff").andExpect(status().isBadRequest());
  }

  // Transfer

  @Test
  void aTransferMovesTheCopyToAnotherLibraryOfTheInstitution() throws Exception {
    long copy = copy();
    long annex = library(institutionId, "Annex");

    long moved =
        read(
            transfer(copy, annex, "librarian").andExpect(status().isCreated()),
            "$.copies[0].copyId");

    assertThat(copyRow(copy)).containsEntry("status", "TRANSFERRED");
    assertThat(labelRow(copy).get("retired_at")).isNotNull();
    assertThat(copyRow(moved))
        .containsEntry("library_id", annex)
        .containsEntry("status", "AVAILABLE")
        .containsEntry("transferred_from_copy_id", copy);
    assertThat(
            jdbc.queryForList(
                """
                SELECT m.library_id, m.type, l.copy_id FROM stock_movement m
                JOIN stock_movement_line l ON l.movement_id = m.id
                WHERE m.reason = 'TRANSFER'"""))
        .extracting(r -> r.get("library_id"), r -> r.get("type"), r -> r.get("copy_id"))
        .containsExactlyInAnyOrder(tuple(library, "EXIT", copy), tuple(annex, "ENTRY", moved));
  }

  @Test
  void transfersStayInsideTheInstitution() throws Exception {
    long other = library(createInstitution(organizationId, "Other school"), "Main");

    transfer(copy(), other, "librarian").andExpect(status().isNotFound());
    transfer(copy(), other, "admin").andExpect(status().isBadRequest());
    transfer(copy(), library, "librarian").andExpect(status().isBadRequest());
  }
}
