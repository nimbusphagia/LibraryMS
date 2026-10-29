package com.nimbusphagia.library_ms.account.internal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class AccountSessionService {
  private final FindByIndexNameSessionRepository<? extends Session> sessions;

  void invalidateAll(String username) {
    sessions.findByPrincipalName(username).keySet().forEach(sessions::deleteById);
  }
}
