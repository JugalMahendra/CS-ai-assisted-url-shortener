package com.urlshortener.shortening.service;

/** No short link exists for the requested code. Intended to map to HTTP 404. */
public class ShortLinkNotFoundException extends RuntimeException {

    public ShortLinkNotFoundException(String code) {
        super("No short link found for code: " + code);
    }
}
