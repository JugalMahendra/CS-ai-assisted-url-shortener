package com.urlshortener.shortening.api;

import com.urlshortener.shortening.domain.LinkStatus;
import java.time.Instant;

/** Response describing a created short link. */
public record LinkResponse(
        String code,
        String shortUrl,
        String originalUrl,
        LinkStatus status,
        Instant createdAt,
        Instant expiresAt) {
}
