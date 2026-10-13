package com.urlshortener.redirect;

import static org.assertj.core.api.Assertions.assertThat;

import com.urlshortener.analytics.repository.ClickEventRepository;
import com.urlshortener.shortening.domain.LinkStatus;
import com.urlshortener.shortening.domain.ShortLink;
import com.urlshortener.shortening.repository.ShortLinkRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** End-to-end redirect + click-recording tests against a real PostgreSQL (requires Docker). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RedirectIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Value("${local.server.port}")
    int port;

    @Autowired
    ShortLinkRepository shortLinkRepository;

    @Autowired
    ClickEventRepository clickEventRepository;

    private HttpResponse<Void> get(String path) throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.discarding());
    }

    @Test
    void activeLinkRedirects302AndRecordsClick() throws Exception {
        ShortLink link = shortLinkRepository.save(new ShortLink("redir01", "https://example.com/dest"));

        HttpResponse<Void> response = get("/redir01");

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location")).hasValue("https://example.com/dest");
        assertThat(shortLinkRepository.findByCode("redir01").orElseThrow().getClickCount()).isEqualTo(1L);
        assertThat(clickEventRepository.countByShortLinkId(link.getId())).isEqualTo(1L);
    }

    @Test
    void unknownCodeReturns404() throws Exception {
        assertThat(get("/nope404").statusCode()).isEqualTo(404);
    }

    @Test
    void expiredLinkReturns410AndIsNotCounted() throws Exception {
        ShortLink link = new ShortLink("exp410x", "https://example.com");
        link.setExpiresAt(Instant.now().minus(Duration.ofHours(1)));
        link = shortLinkRepository.save(link);

        assertThat(get("/exp410x").statusCode()).isEqualTo(410);
        assertThat(shortLinkRepository.findByCode("exp410x").orElseThrow().getClickCount()).isZero();
        assertThat(clickEventRepository.countByShortLinkId(link.getId())).isZero();
    }

    @Test
    void disabledLinkReturns410() throws Exception {
        ShortLink link = new ShortLink("dis410x", "https://example.com");
        link.setStatus(LinkStatus.DISABLED);
        shortLinkRepository.save(link);

        assertThat(get("/dis410x").statusCode()).isEqualTo(410);
    }
}
