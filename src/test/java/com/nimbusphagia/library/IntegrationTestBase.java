package com.nimbusphagia.library;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.nimbusphagia.library.account.internal.entity.Account;
import com.nimbusphagia.library.account.internal.entity.AccountRole;
import com.nimbusphagia.library.account.internal.repository.AccountRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class IntegrationTestBase {

  protected static final String PASSWORD = "Test-pass-123";

  // Reference data inserted by migrations
  private static final List<String> SEEDED_TABLES =
      List.of("flyway_schema_history", "dewey_tag", "visitor_role", "school_grade");

  @Autowired protected MockMvc mockMvc;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected AccountRepository accounts;
  @Autowired protected PasswordEncoder passwordEncoder;
  @Autowired private UserDetailsService userDetailsService;

  protected Long organizationId;
  protected Long institutionId;

  @BeforeEach
  void resetState() {
    List<String> tables =
        jdbc.queryForList(
            "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class);
    jdbc.execute(
        "TRUNCATE "
            + String.join(", ", tables.stream().filter(t -> !SEEDED_TABLES.contains(t)).toList())
            + " RESTART IDENTITY CASCADE");

    organizationId =
        jdbc.queryForObject(
            "INSERT INTO organization (name) VALUES ('Test org') RETURNING id", Long.class);
    institutionId = createInstitution(organizationId, "Test school");

    createAccount("owner", AccountRole.OWNER, true);
    createAccount("admin", AccountRole.ADMIN, true);
    createAccount("librarian", AccountRole.LIBRARIAN, true);
    createAccount("staff", AccountRole.STAFF, true);
  }

  // Authenticates as the stored account, with its real AccountPrincipal
  protected RequestPostProcessor as(String username) {
    return user(userDetailsService.loadUserByUsername(username));
  }

  protected Long createInstitution(Long organizationId, String name) {
    return jdbc.queryForObject(
        "INSERT INTO institution (organization_id, name) VALUES (?, ?) RETURNING id",
        Long.class,
        organizationId,
        name);
  }

  protected Account createAccount(String username, AccountRole role, boolean active) {
    return accounts.save(
        Account.builder()
            .organizationId(organizationId)
            .institutionId(role.isOrganizationWide() ? null : institutionId)
            .username(username)
            .passwordHash(passwordEncoder.encode(PASSWORD))
            .fullName(username)
            .role(role)
            .active(active)
            .build());
  }
}
