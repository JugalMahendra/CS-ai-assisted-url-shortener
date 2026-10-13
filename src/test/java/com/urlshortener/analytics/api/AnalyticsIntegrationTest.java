package com.urlshortener.analytics.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.urlshortener.shortening.domain.ShortLink;
import com.urlshortener.shortening.repository.ShortLinkRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** End-to-end analytics tests against a real PostgreSQL (requires Docker). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AnalyticsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Value("${local.server.port}")
    int port;

    @Autowired
    ShortLinkRepository shortLinkRepository;

    private final HttpClient client =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void analyticsReflectsRecordedClicks() throws Exception {
        shortLinkRepository.save(new ShortLink("an7code", "https://example.com"));
        for (int i = 0; i < 3; i++) {
            assertThat(get("/an7code").statusCode()).isEqualTo(302);
        }

        HttpResponse<String> response = get("/api/v1/links/an7code/analytics");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"code\":\"an7code\"", "\"totalClicks\":3");
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        assertThat(response.body()).contains("\"date\":\"" + today + "\"", "\"clicks\":3");
        assertThat(response.body()).contains("\"firstClickAt\":", "\"lastClickAt\":");
    }

    @Test
    void analyticsForZeroClicks() throws Exception {
        shortLinkRepository.save(new ShortLink("zero7co", "https://example.com"));

        HttpResponse<String> response = get("/api/v1/links/zero7co/analytics");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"totalClicks\":0", "\"timeSeries\":[]");
    }

    @Test
    void analyticsForUnknownCodeReturns404() throws Exception {
        assertThat(get("/api/v1/links/missing/analytics").statusCode()).isEqualTo(404);
    }
}
