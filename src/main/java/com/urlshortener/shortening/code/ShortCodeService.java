package com.urlshortener.shortening.code;

import com.urlshortener.shortening.repository.ShortLinkRepository;
import org.springframework.stereotype.Service;

/**
 * Resolves the short code for a new link: either a validated, currently-available custom alias, or a
 * freshly generated collision-free system code.
 */
@Service
public class ShortCodeService {

    private final ShortCodeGenerator generator;
    private final CustomAliasValidator aliasValidator;
    private final ShortLinkRepository shortLinkRepository;

    public ShortCodeService(ShortCodeGenerator generator,
                            CustomAliasValidator aliasValidator,
                            ShortLinkRepository shortLinkRepository) {
        this.generator = generator;
        this.aliasValidator = aliasValidator;
        this.shortLinkRepository = shortLinkRepository;
    }

    /** Generates a new system short code (collision-free via the sequence). */
    public String generateCode() {
        return generator.nextCode();
    }

    /**
     * Validates a custom alias and checks current availability.
     *
     * <p>This availability check is best-effort (a concurrent request could claim the same alias
     * between this check and the eventual insert); the UNIQUE constraint on {@code short_link.code} is
     * the authoritative guard, and the create-link flow (T5) will translate that constraint violation
     * into an {@link AliasConflictException} as well.
     *
     * @throws InvalidAliasException  if the alias format is invalid or reserved (HTTP 400)
     * @throws AliasConflictException if the alias is already in use (HTTP 409)
     */
    public String claimCustomAlias(String alias) {
        aliasValidator.validate(alias);
        String value = alias.trim();
        if (shortLinkRepository.existsByCode(value)) {
            throw new AliasConflictException("Alias already in use: " + value);
        }
        return value;
    }
}
