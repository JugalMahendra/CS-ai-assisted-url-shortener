package com.urlshortener.shortening.api;

import java.time.Instant;

/**
 * Request to create a short link.
 *
 * @param url         the destination URL (required; validated as absolute http/https)
 * @param customAlias optional user-chosen code; if blank/null a code is generated
 * @param expiresAt   optional expiry instant; must be in the future when provided
 */
public record CreateLinkRequest(String url, String customAlias, Instant expiresAt) {
}
