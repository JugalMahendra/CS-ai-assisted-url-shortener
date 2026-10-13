package com.urlshortener.redirect;

import com.urlshortener.analytics.domain.ClickEvent;
import com.urlshortener.analytics.repository.ClickEventRepository;
import com.urlshortener.shortening.domain.LinkStatus;
import com.urlshortener.shortening.domain.ShortLink;
import com.urlshortener.shortening.repository.ShortLinkRepository;
import com.urlshortener.shortening.service.ShortLinkNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves a short code to its destination and records the click. The resolution checks and the two
 * writes (increment the denormalized counter + insert the click event) run in a single transaction,
 * so {@code short_link.click_count} stays consistent with {@code click_event} rows. No message queue
 * or async processing is used (per the approved design).
 */
@Service
public class RedirectService {

    private final ShortLinkRepository shortLinkRepository;
    private final ClickEventRepository clickEventRepository;

    public RedirectService(ShortLinkRepository shortLinkRepository,
                           ClickEventRepository clickEventRepository) {
        this.shortLinkRepository = shortLinkRepository;
        this.clickEventRepository = clickEventRepository;
    }

    /**
     * @return the destination URL to redirect to
     * @throws ShortLinkNotFoundException if no link exists for {@code code} (HTTP 404)
     * @throws ShortLinkGoneException     if the link is disabled or expired (HTTP 410)
     */
    @Transactional
    public String resolveAndRecordClick(String code, String referrer, String userAgent, String clientIp) {
        ShortLink link = shortLinkRepository.findByCode(code)
                .orElseThrow(() -> new ShortLinkNotFoundException(code));

        if (link.getStatus() == LinkStatus.DISABLED) {
            throw new ShortLinkGoneException(code, "disabled");
        }
        if (link.getStatus() == LinkStatus.EXPIRED || isExpired(link.getExpiresAt(), Instant.now())) {
            throw new ShortLinkGoneException(code, "expired");
        }

        shortLinkRepository.incrementClickCount(link.getId());
        clickEventRepository.save(newClickEvent(link.getId(), referrer, userAgent, clientIp));

        return link.getOriginalUrl();
    }

    /** Lazy expiry: a link with an {@code expiresAt} at or before now is treated as expired. */
    static boolean isExpired(Instant expiresAt, Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    private static ClickEvent newClickEvent(Long shortLinkId, String referrer, String userAgent, String clientIp) {
        ClickEvent event = new ClickEvent(shortLinkId);
        event.setReferrer(referrer);
        event.setUserAgent(userAgent);
        event.setIpHash(hashIp(clientIp));
        return event;
    }

    /** SHA-256 hex of the client IP (store a hash, not the raw address, for privacy). */
    static String hashIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(clientIp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
