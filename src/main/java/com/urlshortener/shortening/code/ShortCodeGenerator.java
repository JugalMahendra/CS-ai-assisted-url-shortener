package com.urlshortener.shortening.code;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Generates short codes by Base62-encoding values drawn from the dedicated {@code short_code_seq}
 * PostgreSQL sequence. Sequence values are unique and monotonic (and {@code nextval} is safe under
 * concurrency), so generated codes are collision-free by construction; the UNIQUE constraint on
 * {@code short_link.code} is the safety net.
 */
@Component
public class ShortCodeGenerator {

    private final JdbcTemplate jdbcTemplate;

    public ShortCodeGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String nextCode() {
        Long value = jdbcTemplate.queryForObject("SELECT nextval('short_code_seq')", Long.class);
        if (value == null) {
            throw new IllegalStateException("short_code_seq returned no value");
        }
        return Base62.encode(value);
    }
}
