package com.urlshortener.shortening.service;

import com.urlshortener.shortening.api.CreateLinkRequest;
import com.urlshortener.shortening.api.LinkResponse;
import com.urlshortener.shortening.code.AliasConflictException;
import com.urlshortener.shortening.code.ShortCodeService;
import com.urlshortener.shortening.domain.ShortLink;
import com.urlshortener.shortening.repository.ShortLinkRepository;
import com.urlshortener.validation.UrlValidator;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Creates short links by composing URL validation (T3), short-code/alias resolution (T4), and
 * persistence (T2). Not {@code @Transactional}: each {@code save} runs in its own transaction so the
 * generated-code retry can proceed after a (very rare) unique-constraint collision.
 */
@Service
public class LinkService {

    private static final int MAX_CODE_ATTEMPTS = 5;

    private final UrlValidator urlValidator;
    private final ShortCodeService shortCodeService;
    private final ShortLinkRepository repository;
    private final String baseUrl;

    public LinkService(UrlValidator urlValidator,
                       ShortCodeService shortCodeService,
                       ShortLinkRepository repository,
                       @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.urlValidator = urlValidator;
        this.shortCodeService = shortCodeService;
        this.repository = repository;
        this.baseUrl = stripTrailingSlash(baseUrl);
    }

    public LinkResponse create(CreateLinkRequest request) {
        String url = urlValidator.validate(request.url());
        Instant expiresAt = validateExpiry(request.expiresAt());

        boolean hasAlias = request.customAlias() != null && !request.customAlias().isBlank();
        ShortLink saved = hasAlias
                ? createWithCustomAlias(request.customAlias(), url, expiresAt)
                : createWithGeneratedCode(url, expiresAt);

        return toResponse(saved);
    }

    private ShortLink createWithCustomAlias(String alias, String url, Instant expiresAt) {
        String code = shortCodeService.claimCustomAlias(alias); // validates format + pre-checks availability
        ShortLink link = new ShortLink(code, url);
        link.setCustomAlias(true);
        link.setExpiresAt(expiresAt);
        try {
            return repository.save(link);
        } catch (DataIntegrityViolationException e) {
            // Lost the check-then-insert race: another request claimed this alias first.
            throw new AliasConflictException("Alias already in use: " + code);
        }
    }

    private ShortLink createWithGeneratedCode(String url, Instant expiresAt) {
        for (int attempt = 1; attempt <= MAX_CODE_ATTEMPTS; attempt++) {
            String code = shortCodeService.generateCode();
            ShortLink link = new ShortLink(code, url);
            link.setExpiresAt(expiresAt);
            try {
                return repository.save(link);
            } catch (DataIntegrityViolationException e) {
                // Extremely rare: a generated code collided with an existing (e.g. custom) code. Retry.
                if (attempt == MAX_CODE_ATTEMPTS) {
                    throw new IllegalStateException(
                            "Could not generate a unique short code after " + MAX_CODE_ATTEMPTS + " attempts", e);
                }
            }
        }
        throw new IllegalStateException("unreachable");
    }

    private Instant validateExpiry(Instant expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new InvalidExpirationException("expiresAt must be in the future");
        }
        return expiresAt;
    }

    private LinkResponse toResponse(ShortLink link) {
        return new LinkResponse(
                link.getCode(),
                baseUrl + "/" + link.getCode(),
                link.getOriginalUrl(),
                link.getStatus(),
                link.getCreatedAt(),
                link.getExpiresAt());
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
