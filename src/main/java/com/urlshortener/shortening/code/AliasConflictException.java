package com.urlshortener.shortening.code;

/** A custom alias (or short code) is already in use. Intended to map to HTTP 409. */
public class AliasConflictException extends RuntimeException {

    public AliasConflictException(String message) {
        super(message);
    }
}
