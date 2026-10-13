package com.urlshortener.shortening.code;

import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Validates user-supplied custom aliases for format and reserved words only. Availability (collision
 * against existing codes) is checked separately against the datastore. Pure and reusable.
 */
@Component
public class CustomAliasValidator {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 16;

    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9_-]+");
    private static final Set<String> RESERVED =
            Set.of("api", "actuator", "health", "favicon.ico", "robots.txt");

    public void validate(String alias) {
        if (alias == null || alias.isBlank()) {
            throw new InvalidAliasException("Alias must not be blank");
        }
        String value = alias.trim();
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidAliasException(
                    "Alias length must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters");
        }
        if (!ALLOWED.matcher(value).matches()) {
            throw new InvalidAliasException("Alias may contain only letters, digits, '-' and '_'");
        }
        if (RESERVED.contains(value.toLowerCase())) {
            throw new InvalidAliasException("Alias is reserved: " + value);
        }
    }
}
