package com.urlshortener.shortening.service;

/** A requested expiration is invalid (e.g. not in the future). Intended to map to HTTP 400. */
public class InvalidExpirationException extends RuntimeException {

    public InvalidExpirationException(String message) {
        super(message);
    }
}
