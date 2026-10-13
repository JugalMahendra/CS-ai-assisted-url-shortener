package com.urlshortener.redirect;

/** A short link exists but is no longer served (expired or deactivated). Intended to map to HTTP 410. */
public class ShortLinkGoneException extends RuntimeException {

    public ShortLinkGoneException(String code, String reason) {
        super("Short link '" + code + "' is gone (" + reason + ")");
    }
}
