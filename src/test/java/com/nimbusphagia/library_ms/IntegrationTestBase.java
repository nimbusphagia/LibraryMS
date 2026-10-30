package com.nimbusphagia.library_ms;

import com.nimbusphagia.library_ms.account.internal.entity.Account;
import com.nimbusphagia.library_ms.account.internal.entity.AccountRole;
import com.nimbusphagia.library_ms.account.internal.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

  protected static final String OWNER_USERNAME = "owner";
  protected static final String OWNER_PASSWORD = "test-password";
  protected static final String PASSWORD = "Test-pass-123";

  @Autowired protected MockMvc mockMvc;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected AccountRepository accounts;
  @Autowired protected PasswordEncoder passwordEncoder;

  @BeforeEach
  void resetState() {
    jdbc.execute(
        "TRUNCATE book_tag, book_collaborator, book, collection, tag, publisher, author"
            + " RESTART IDENTITY");
    jdbc.update("DELETE FROM spring_session_attributes");
    jdbc.update("DELETE FROM spring_session");
    jdbc.update("DELETE FROM account WHERE role <> 'OWNER'");

    createAccount("admin", AccountRole.ADMIN, true);
    createAccount("staff", AccountRole.STAFF, true);
  }

  protected Account createAccount(String username, AccountRole role, boolean active) {
    return accounts.save(
        Account.builder()
            .username(username)
            .passwordHash(passwordEncoder.encode(PASSWORD))
            .fullName(username)
            .role(role)
            .active(active)
            .build());
  }
}
