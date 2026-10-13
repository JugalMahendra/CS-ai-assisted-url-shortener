package com.urlshortener.shortening.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.urlshortener.shortening.repository.ShortLinkRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end create-link tests against a real PostgreSQL over HTTP (requires Docker). Uses the JDK
 * HttpClient against a RANDOM_PORT server to avoid the Spring Boot MVC test-slice module.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CreateLinkIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Value("${local.server.port}")
    int port;

    @Autowired
    ShortLinkRepository repository;

    private HttpResponse<String> postJson(String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/links"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void createsLinkWithGeneratedCode() throws Exception {
        HttpResponse<String> response = postJson("{\"url\":\"https://example.com/page\"}");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body())
                .contains("\"originalUrl\":\"https://example.com/page\"", "\"status\":\"ACTIVE\"");
    }

    @Test
    void createsLinkWithCustomAlias() throws Exception {
        HttpResponse<String> response =
                postJson("{\"url\":\"https://example.com\",\"customAlias\":\"my-promo\"}");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains("\"code\":\"my-promo\"");
        assertThat(repository.existsByCode("my-promo")).isTrue();
    }

    @Test
    void duplicateAliasReturns409() throws Exception {
        String body = "{\"url\":\"https://example.com\",\"customAlias\":\"dupe-alias\"}";
        assertThat(postJson(body).statusCode()).isEqualTo(201);
        assertThat(postJson(body).statusCode()).isEqualTo(409);
    }

    @Test
    void unsafeSchemeReturns422() throws Exception {
        assertThat(postJson("{\"url\":\"file:///etc/passwd\"}").statusCode()).isEqualTo(422);
    }

    @Test
    void malformedUrlReturns400() throws Exception {
        assertThat(postJson("{\"url\":\"not a url\"}").statusCode()).isEqualTo(400);
    }
}
