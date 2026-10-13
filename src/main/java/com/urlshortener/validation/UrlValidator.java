package com.urlshortener.validation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validates destination URLs for the shortener. Pure and side-effect-free, with no controller or
 * database dependencies, so it can be reused directly by the create-link API.
 *
 * <p><strong>This never fetches, resolves (DNS), or connects to the target URL.</strong> The
 * unsafe-target checks therefore operate on the literal host string only and provide
 * <em>partial</em> SSRF / open-redirect protection. Residual risks are documented on
 * {@link #isDisallowedHost(String)} and in the README; callers must not treat a passing result as a
 * guarantee that the destination is safe.
 */
@Component
public class UrlValidator {

    /** Maximum accepted URL length; bounds storage and limits abuse. */
    public static final int MAX_LENGTH = 2048;

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    /**
     * Validates {@code rawUrl} and returns it trimmed.
     *
     * @throws MalformedUrlException if null/blank, too long, unparseable, not absolute, or missing a host
     * @throws UnsafeUrlException    if the scheme is not http/https, or the host is a literal
     *                               loopback/private/link-local/unspecified address
     */
    public String validate(String rawUrl) {
        if (rawUrl == null) {
            throw new MalformedUrlException("URL must not be null");
        }
        String url = rawUrl.trim();
        if (url.isEmpty()) {
            throw new MalformedUrlException("URL must not be blank");
        }
        if (url.length() > MAX_LENGTH) {
            throw new MalformedUrlException("URL exceeds maximum length of " + MAX_LENGTH);
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new MalformedUrlException("URL is not parseable: " + e.getReason());
        }

        if (!uri.isAbsolute()) {
            throw new MalformedUrlException("URL must be absolute (include a scheme)");
        }

        String scheme = uri.getScheme().toLowerCase();
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            throw new UnsafeUrlException("Unsupported URL scheme '" + scheme + "' (only http/https are allowed)");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new MalformedUrlException("URL must include a valid host");
        }

        if (isDisallowedHost(host)) {
            throw new UnsafeUrlException("URL host is not allowed: " + host);
        }

        return url;
    }

    /**
     * Rejects literal loopback/private/link-local/unspecified hosts.
     *
     * <p>Best-effort only: this inspects the <em>literal</em> host and performs no DNS resolution
     * (we never connect to the target). Known residual risks NOT covered here:
     * <ul>
     *   <li>a public hostname that resolves to a private/internal address (and DNS rebinding, where
     *       resolution changes between validation and a later redirect);</li>
     *   <li>alternative IP encodings (decimal e.g. {@code http://2130706433}, octal, hex, IPv4-mapped
     *       IPv6) that are not dotted-quad literals;</li>
     *   <li>the inherent open-redirect nature of a shortener (it forwards users to arbitrary public
     *       destinations by design).</li>
     * </ul>
     */
    private boolean isDisallowedHost(String rawHost) {
        String host = rawHost.toLowerCase();
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1); // strip IPv6 brackets
        }
        if (host.equals("localhost") || host.endsWith(".localhost")) {
            return true;
        }
        if (isIpv4Literal(host)) {
            return isDisallowedIpv4(host);
        }
        if (host.indexOf(':') >= 0) {
            return isDisallowedIpv6(host);
        }
        return false;
    }

    private boolean isIpv4Literal(String host) {
        return host.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    }

    private boolean isDisallowedIpv4(String host) {
        String[] parts = host.split("\\.");
        int[] o = new int[4];
        for (int i = 0; i < 4; i++) {
            int value = Integer.parseInt(parts[i]);
            if (value > 255) {
                return true; // invalid octet -> reject to be safe
            }
            o[i] = value;
        }
        return o[0] == 0                                     // 0.0.0.0/8 (incl. unspecified)
                || o[0] == 127                               // loopback 127.0.0.0/8
                || o[0] == 10                                // private 10.0.0.0/8
                || (o[0] == 172 && o[1] >= 16 && o[1] <= 31) // private 172.16.0.0/12
                || (o[0] == 192 && o[1] == 168)              // private 192.168.0.0/16
                || (o[0] == 169 && o[1] == 254)              // link-local 169.254.0.0/16 (cloud metadata)
                || (o[0] == 100 && o[1] >= 64 && o[1] <= 127); // CGNAT 100.64.0.0/10
    }

    private boolean isDisallowedIpv6(String host) {
        return host.equals("::1")          // loopback
                || host.equals("::")       // unspecified
                || host.startsWith("fe80") // link-local fe80::/10
                || host.startsWith("fc")   // unique local fc00::/7
                || host.startsWith("fd");
    }
}
