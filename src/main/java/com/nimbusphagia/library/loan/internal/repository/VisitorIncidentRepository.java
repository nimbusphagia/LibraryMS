package com.nimbusphagia.library.loan.internal.repository;

import com.nimbusphagia.library.loan.internal.entity.VisitorIncident;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitorIncidentRepository extends JpaRepository<VisitorIncident, Long> {

  interface History {
    Long getVisitorId();

    long getPoints();

    long getSerious();
  }

  @EntityGraph(attributePaths = {"type", "recordedBy"})
  List<VisitorIncident> findByVisitorIdOrderByCreatedAtDesc(Long visitorId);

  Optional<VisitorIncident> findByIdAndVisitorIdAndInstitutionId(
      Long id, Long visitorId, Long institutionId);

  @Query(
      nativeQuery = true,
      value =
          """
          SELECT i.visitor_id AS visitorId,
                 SUM(t.weight) AS points,
                 COUNT(*) FILTER (WHERE t.serious) AS serious
          FROM visitor_incident i
          JOIN visitor_incident_type t ON t.id = i.type_id
          WHERE i.visitor_id IN (:visitorIds) AND i.created_at >= :since
          GROUP BY i.visitor_id
          """)
  List<History> findHistory(
      @Param("visitorIds") Collection<Long> visitorIds, @Param("since") Instant since);
}
