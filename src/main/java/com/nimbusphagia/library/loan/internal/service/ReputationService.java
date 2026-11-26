package com.nimbusphagia.library.loan.internal.service;

import com.nimbusphagia.library.loan.internal.entity.Reputation;
import com.nimbusphagia.library.loan.internal.repository.LoanMovementLineRepository;
import com.nimbusphagia.library.loan.internal.repository.VisitorIncidentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReputationService {
  private final LoanMovementLineRepository lineRepository;
  private final VisitorIncidentRepository incidentRepository;
  private final Clock clock;

  @Transactional(readOnly = true)
  public Map<Long, Reputation> of(Collection<Long> visitorIds) {
    if (visitorIds.isEmpty()) return Map.of();
    Instant since = ZonedDateTime.now(clock).minusMonths(Reputation.HISTORY_MONTHS).toInstant();
    Map<Long, LoanMovementLineRepository.History> lines =
        lineRepository.findHistory(visitorIds, since).stream()
            .collect(
                Collectors.toMap(
                    LoanMovementLineRepository.History::getVisitorId, Function.identity()));
    Map<Long, VisitorIncidentRepository.History> incidents =
        incidentRepository.findHistory(visitorIds, since).stream()
            .collect(
                Collectors.toMap(
                    VisitorIncidentRepository.History::getVisitorId, Function.identity()));

    return visitorIds.stream()
        .distinct()
        .collect(
            Collectors.toMap(
                Function.identity(),
                id -> {
                  var l = lines.get(id);
                  var i = incidents.get(id);
                  return Reputation.of(
                      l == null ? 0 : l.getResolved(),
                      l == null ? 0 : l.getLate(),
                      l == null ? 0 : l.getDamaged(),
                      l == null ? 0 : l.getLost(),
                      i == null ? 0 : i.getPoints(),
                      i == null ? 0 : i.getSerious());
                }));
  }

  public Reputation of(Long visitorId) {
    return of(List.of(visitorId)).get(visitorId);
  }
}
