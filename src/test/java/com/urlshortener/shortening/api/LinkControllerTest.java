package com.urlshortener.shortening.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.urlshortener.shortening.domain.LinkStatus;
import com.urlshortener.shortening.service.LinkService;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Pure unit test for {@link LinkController} using a hand-written service stub (no Mockito/Spring). */
class LinkControllerTest {

    private static final LinkResponse RESPONSE = new LinkResponse("abc1234",
            "http://localhost:8080/abc1234", "https://example.com", LinkStatus.ACTIVE,
            Instant.parse("2026-01-01T00:00:00Z"), null);

    private final LinkController controller = new LinkController(new StubLinkService());

    @Test
    void createReturns201WithLocationAndBody() {
        ResponseEntity<LinkResponse> result =
                controller.create(new CreateLinkRequest("https://example.com", null, null));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getHeaders().getLocation())
                .isEqualTo(URI.create("http://localhost:8080/abc1234"));
        assertThat(result.getBody()).isEqualTo(RESPONSE);
    }

    /** Minimal stub avoiding Mockito (its inline mock-maker can't self-attach in some sandboxes). */
    private static final class StubLinkService extends LinkService {
        StubLinkService() {
            super(null, null, null, "http://localhost:8080");
        }

        @Override
        public LinkResponse create(CreateLinkRequest request) {
            return RESPONSE;
        }
    }
}
