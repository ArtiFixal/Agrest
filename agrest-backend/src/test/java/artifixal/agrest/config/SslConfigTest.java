package artifixal.agrest.config;

import artifixal.agrest.common.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests if {@code SslConfig} indicates upgrade requirement.
 */
public class SslConfigTest extends IntegrationTest{

    @Test
    public void shouldAskForUpgrade(){
        http.mutate()
            .baseUrl("http://localhost:8080")
            .build()
            .post()
            .uri("/any")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UPGRADE_REQUIRED);
    }
}
