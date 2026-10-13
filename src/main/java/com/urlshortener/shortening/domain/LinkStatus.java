package com.urlshortener.shortening.domain;

/** Lifecycle state of a {@link ShortLink}. */
public enum LinkStatus {
    ACTIVE,
    EXPIRED,
    DISABLED
}
