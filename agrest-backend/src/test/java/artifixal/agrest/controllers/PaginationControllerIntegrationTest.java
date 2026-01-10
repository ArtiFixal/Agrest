package artifixal.agrest.controllers;

import artifixal.agrest.auth.WithMockToken;
import artifixal.agrest.common.IntegrationTest;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

/**
 * Integration tests of {@code PaginationController}.
 */
public class PaginationControllerIntegrationTest extends IntegrationTest {

    @Test
    @WithMockToken
    public void getPageSizes() {
        var response = http.get()
            .uri("/v1/pagination")
            .exchangeSuccessfully()
            .expectBodyList(Integer.class)
            .returnResult()
            .getResponseBody();
        assertNotEquals(0, response.size());
    }
}
