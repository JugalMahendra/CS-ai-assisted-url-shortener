package com.urlshortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urlshortener.analytics.domain.ClickEvent;
import com.urlshortener.analytics.repository.ClickEventRepository;
import com.urlshortener.shortening.domain.LinkStatus;
import com.urlshortener.shortening.domain.ShortLink;
import com.urlshortener.shortening.repository.ShortLinkRepository;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * T2 data-model verification against a real PostgreSQL: Flyway V2 is applied, the entities map to
 * the schema, the unique short-code constraint is enforced, expiration is stored, and click events
 * persist against a link.
 */
@SpringBootTest
@Testcontainers
class DataModelPersistenceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    ShortLinkRepository shortLinks;

    @Autowired
    ClickEventRepository clickEvents;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayRecordsMigrationV2() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '2' and success",
                Integer.class);
        assertThat(applied).isEqualTo(1);
    }

    @Test
    void savesAndFindsShortLinkByCodeWithDefaults() {
        ShortLink saved = shortLinks.save(new ShortLink("abc1234", "https://example.com/a/very/long/path"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(LinkStatus.ACTIVE);
        assertThat(saved.getClickCount()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(shortLinks.findByCode("abc1234")).isPresent();
        assertThat(shortLinks.existsByCode("abc1234")).isTrue();
    }

    @Test
    void enforcesUniqueShortCode() {
        shortLinks.saveAndFlush(new ShortLink("dupcode", "https://a.example"));

        assertThatThrownBy(() -> shortLinks.saveAndFlush(new ShortLink("dupcode", "https://b.example")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void storesExpiration() {
        ShortLink link = new ShortLink("exp0001", "https://example.com");
        link.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));

        ShortLink saved = shortLinks.save(link);

        assertThat(shortLinks.findById(saved.getId()).orElseThrow().getExpiresAt()).isNotNull();
    }

    @Test
    void recordsClickEventsForLink() {
        ShortLink link = shortLinks.save(new ShortLink("clk0001", "https://example.com"));
        ClickEvent event = new ClickEvent(link.getId());
        event.setReferrer("https://ref.example");
        event.setUserAgent("JUnit");
        event.setIpHash("0123456789abcdef");

        clickEvents.save(event);

        assertThat(clickEvents.countByShortLinkId(link.getId())).isEqualTo(1L);
    }
}
