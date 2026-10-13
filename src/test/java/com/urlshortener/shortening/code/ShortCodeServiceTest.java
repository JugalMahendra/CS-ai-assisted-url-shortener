package com.urlshortener.shortening.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urlshortener.shortening.domain.ShortLink;
import com.urlshortener.shortening.repository.ShortLinkRepository;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for sequence-based code generation and custom-alias collision handling against a
 * real PostgreSQL (requires Docker; exercises Flyway V3's short_code_seq). Named {@code *Test} so it
 * runs under Surefire, consistent with the project's other Testcontainers tests.
 */
@SpringBootTest
@Testcontainers
class ShortCodeServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    ShortCodeService service;

    @Autowired
    ShortLinkRepository repository;

    @Test
    void generatesUniqueSevenCharCodes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String code = service.generateCode();
            assertThat(code).hasSize(7);
            assertThat(codes.add(code)).as("code %s should be unique", code).isTrue();
        }
    }

    @Test
    void claimCustomAliasAcceptsFreeAlias() {
        assertThat(service.claimCustomAlias("my-promo")).isEqualTo("my-promo");
    }

    @Test
    void claimCustomAliasRejectsTakenAlias() {
        repository.save(new ShortLink("takenalias", "https://example.com"));

        assertThatThrownBy(() -> service.claimCustomAlias("takenalias"))
                .isInstanceOf(AliasConflictException.class);
    }

    @Test
    void claimCustomAliasRejectsInvalidFormat() {
        assertThatThrownBy(() -> service.claimCustomAlias("has space"))
                .isInstanceOf(InvalidAliasException.class);
    }
}
