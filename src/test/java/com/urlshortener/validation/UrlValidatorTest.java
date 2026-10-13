package com.urlshortener.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Focused unit tests for {@link UrlValidator}. Pure (no Spring context, no database, no network).
 */
class UrlValidatorTest {

    private final UrlValidator validator = new UrlValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com",
            "https://example.com",
            "https://example.com/a/very/long/path?x=1&y=2#frag",
            "https://sub.example.co.uk:8443/path",
            "HTTP://Example.COM/Case",   // scheme is case-insensitive
            "https://93.184.216.34/",    // public IP literal is allowed
            " https://example.com "      // surrounding whitespace is trimmed
    })
    void acceptsValidHttpAndHttpsUrls(String url) {
        assertThat(validator.validate(url)).isNotBlank();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "   ",
            "not a url",
            "example.com",      // no scheme
            "//example.com",    // scheme-relative, not absolute
            "/relative/path",
            "http://",          // no host
            "http:///path",     // empty host
            "https://example.com/<bad>"  // illegal characters -> unparseable
    })
    void rejectsMalformedUrls(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(MalformedUrlException.class);
    }

    @Test
    void rejectsUrlExceedingMaxLength() {
        String tooLong = "https://example.com/" + "a".repeat(UrlValidator.MAX_LENGTH);

        assertThatThrownBy(() -> validator.validate(tooLong))
                .isInstanceOf(MalformedUrlException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "file:///etc/passwd",
            "ftp://example.com/file",
            "data:text/plain,hello",
            "mailto:user@example.com"
    })
    void rejectsUnsafeOrUnsupportedSchemes(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(UnsafeUrlException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost/",
            "http://admin.localhost/",
            "http://127.0.0.1/",
            "http://10.0.0.5/",
            "http://172.16.3.4/",
            "http://192.168.1.1/",
            "http://169.254.169.254/latest/meta-data",  // cloud metadata endpoint
            "http://0.0.0.0/",
            "http://[::1]/",
            "https://[fe80::1]/"
    })
    void rejectsLoopbackPrivateAndLinkLocalHosts(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(UnsafeUrlException.class);
    }
}
