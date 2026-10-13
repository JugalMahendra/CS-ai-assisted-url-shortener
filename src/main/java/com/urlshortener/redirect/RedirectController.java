package com.urlshortener.redirect;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Resolves a short code at the root path and issues a redirect to the destination URL. */
@RestController
public class RedirectController {

    private final RedirectService redirectService;
    private final int redirectStatus;

    public RedirectController(RedirectService redirectService,
                             @Value("${app.redirect-status:302}") int redirectStatus) {
        this.redirectService = redirectService;
        this.redirectStatus = redirectStatus;
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        String target = redirectService.resolveAndRecordClick(
                code,
                request.getHeader("Referer"),
                request.getHeader("User-Agent"),
                request.getRemoteAddr());
        return ResponseEntity.status(redirectStatus).location(URI.create(target)).build();
    }
}
