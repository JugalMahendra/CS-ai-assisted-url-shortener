package com.urlshortener;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T1 smoke test.
 *
 * <p>Starts the full application against a real PostgreSQL provided by Testcontainers (wired via
 * {@link ServiceConnection}). This proves: the Spring context loads, the DataSource connects to a
 * real PostgreSQL, Flyway applies the baseline migration at startup, and the health endpoint is
 * reachable and reports {@code UP}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class UrlShortenerApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Value("${local.server.port}")
    int port;

    @Test
    void contextLoads() {
        // Intentionally empty: a passing context load confirms DataSource connectivity and that
        // Flyway applied V1 during startup. Failures surface here as context initialization errors.
    }

    @Test
    void healthEndpointReportsStatusUp() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/actuator/health"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        // show-details: never -> aggregate status only, no component details exposed.
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }
}
