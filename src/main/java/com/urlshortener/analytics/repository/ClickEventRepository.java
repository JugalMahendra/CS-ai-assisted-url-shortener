package com.urlshortener.analytics.repository;

import com.urlshortener.analytics.domain.ClickEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByShortLinkId(Long shortLinkId);

    Optional<ClickEvent> findFirstByShortLinkIdOrderByOccurredAtAsc(Long shortLinkId);

    Optional<ClickEvent> findFirstByShortLinkIdOrderByOccurredAtDesc(Long shortLinkId);

    /**
     * Per-day click counts (UTC) for a link, ascending. Native (PostgreSQL) query; the day is returned
     * as an ISO 'YYYY-MM-DD' string to avoid JDBC date-type mapping ambiguity in interface projections.
     */
    @Query(value = """
            SELECT to_char(occurred_at AT TIME ZONE 'UTC', 'YYYY-MM-DD') AS day, COUNT(*) AS clicks
            FROM click_event
            WHERE short_link_id = :id
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<DailyClickCountProjection> findDailyCounts(@Param("id") Long shortLinkId);
}
