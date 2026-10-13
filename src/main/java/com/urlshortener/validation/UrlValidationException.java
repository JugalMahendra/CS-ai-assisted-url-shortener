package com.urlshortener.validation;

/** Base type for URL validation failures. */
public class UrlValidationException extends RuntimeException {

    public UrlValidationException(String message) {
        super(message);
    }
}
