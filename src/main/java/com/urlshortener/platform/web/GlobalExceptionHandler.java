package com.urlshortener.platform.web;

import com.urlshortener.redirect.ShortLinkGoneException;
import com.urlshortener.shortening.code.AliasConflictException;
import com.urlshortener.shortening.code.InvalidAliasException;
import com.urlshortener.shortening.service.InvalidExpirationException;
import com.urlshortener.shortening.service.ShortLinkNotFoundException;
import com.urlshortener.validation.MalformedUrlException;
import com.urlshortener.validation.UnsafeUrlException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain exceptions to RFC 7807 problem responses (application/problem+json). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MalformedUrlException.class)
    ProblemDetail handleMalformedUrl(MalformedUrlException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid URL", ex.getMessage());
    }

    @ExceptionHandler(UnsafeUrlException.class)
    ProblemDetail handleUnsafeUrl(UnsafeUrlException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Unsafe URL", ex.getMessage());
    }

    @ExceptionHandler(InvalidAliasException.class)
    ProblemDetail handleInvalidAlias(InvalidAliasException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid alias", ex.getMessage());
    }

    @ExceptionHandler(InvalidExpirationException.class)
    ProblemDetail handleInvalidExpiration(InvalidExpirationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid expiration", ex.getMessage());
    }

    @ExceptionHandler(AliasConflictException.class)
    ProblemDetail handleAliasConflict(AliasConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Alias conflict", ex.getMessage());
    }

    @ExceptionHandler(ShortLinkNotFoundException.class)
    ProblemDetail handleNotFound(ShortLinkNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage());
    }

    @ExceptionHandler(ShortLinkGoneException.class)
    ProblemDetail handleGone(ShortLinkGoneException ex) {
        return problem(HttpStatus.GONE, "Gone", ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        return problemDetail;
    }
}
