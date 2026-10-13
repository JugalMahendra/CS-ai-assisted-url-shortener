package com.urlshortener.validation;

/**
 * The URL is syntactically invalid: null/blank, too long, unparseable, not absolute, or missing a
 * host. Intended to map to an HTTP 400 by the create-link API.
 */
public class MalformedUrlException extends UrlValidationException {

    public MalformedUrlException(String message) {
        super(message);
    }
}
