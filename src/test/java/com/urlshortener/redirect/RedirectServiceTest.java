package com.urlshortener.redirect;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Pure unit tests for {@link RedirectService}'s stateless helpers (no Spring/DB). */
class RedirectServiceTest {

    @Test
    void isExpiredHandlesNullFuturePastAndNow() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        assertThat(RedirectService.isExpired(null, now)).isFalse();
        assertThat(RedirectService.isExpired(now.plus(Duration.ofHours(1)), now)).isFalse();
        assertThat(RedirectService.isExpired(now.minus(Duration.ofHours(1)), now)).isTrue();
        assertThat(RedirectService.isExpired(now, now)).isTrue(); // exactly now counts as expired
    }

    @Test
    void hashIpIsStableHexOrNull() {
        assertThat(RedirectService.hashIp(null)).isNull();
        assertThat(RedirectService.hashIp("   ")).isNull();

        String hash = RedirectService.hashIp("203.0.113.9");
        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(RedirectService.hashIp("203.0.113.9")).isEqualTo(hash);
        assertThat(RedirectService.hashIp("203.0.113.10")).isNotEqualTo(hash);
    }
}
