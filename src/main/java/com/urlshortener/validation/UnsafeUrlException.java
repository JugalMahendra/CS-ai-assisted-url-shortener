package com.urlshortener.validation;

/**
 * The URL is syntactically valid but points at a disallowed scheme (anything other than http/https)
 * or a disallowed literal host (loopback/private/link-local/unspecified). Intended to map to an
 * HTTP 422 by the create-link API.
 */
public class UnsafeUrlException extends UrlValidationException {

    public UnsafeUrlException(String message) {
        super(message);
    }
}
