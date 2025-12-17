package artifixal.agrest.controller;

import artifixal.agrest.services.CsrfService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * REST controller providing anti CSRF tokens.
 */
@RestController
@AllArgsConstructor
public class CsrfController {

    private final CsrfService csrfService;

    @GetMapping("/v1/csrf")
    public Mono<ServerResponse> getCsrfToken() {
        return csrfService.generateToken()
            .flatMap((token) -> ServerResponse.ok()
                .cookie(ResponseCookie.from("csrf", token)
                    .path("/")
                    .sameSite("Strict")
                    .secure(true)
                    .maxAge(180)
                    .build())
                .build());
    }
}
