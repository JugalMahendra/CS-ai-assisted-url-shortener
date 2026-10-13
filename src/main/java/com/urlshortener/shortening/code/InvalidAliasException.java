package com.urlshortener.shortening.code;

/** A custom alias is syntactically invalid or reserved. Intended to map to HTTP 400. */
public class InvalidAliasException extends RuntimeException {

    public InvalidAliasException(String message) {
        super(message);
    }
}
