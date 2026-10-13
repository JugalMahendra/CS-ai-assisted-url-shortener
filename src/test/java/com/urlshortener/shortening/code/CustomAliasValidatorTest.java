package com.urlshortener.shortening.code;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Pure unit tests for {@link CustomAliasValidator} (format + reserved words; no database). */
class CustomAliasValidatorTest {

    private final CustomAliasValidator validator = new CustomAliasValidator();

    @ParameterizedTest
    @ValueSource(strings = {"abc", "my-promo", "My_Alias1", "a1b2c3", "sixteen_chars_16"})
    void acceptsValidAliases(String alias) {
        assertThatCode(() -> validator.validate(alias)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "  ",                   // blank
            "ab",                   // too short
            "seventeen_chars_1x",   // too long (18)
            "has space",            // illegal char
            "bad!char",             // illegal char
            "dot.alias",            // illegal char
            "api", "ACTUATOR", "health"  // reserved (case-insensitive)
    })
    void rejectsInvalidOrReservedAliases(String alias) {
        assertThatThrownBy(() -> validator.validate(alias))
                .isInstanceOf(InvalidAliasException.class);
    }
}
