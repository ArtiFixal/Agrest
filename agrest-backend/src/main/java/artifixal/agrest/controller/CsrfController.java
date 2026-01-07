package artifixal.agrest.controller;

import artifixal.agrest.services.CsrfService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * REST controller providing anti CSRF tokens.
 */
@RestController
@AllArgsConstructor
public class CsrfController {

    private final CsrfService csrfService;

    @GetMapping("/v1/csrf")
    public Mono<Void> getCsrfToken(ServerWebExchange exchange) {
        return csrfService.generateToken()
            .map((token) -> csrfService.createCsrfCookie(token))
            .flatMap((csrfCookie) -> {
                exchange.getResponse().addCookie(csrfCookie);
                return exchange.getResponse().setComplete();
            });
    }
}
