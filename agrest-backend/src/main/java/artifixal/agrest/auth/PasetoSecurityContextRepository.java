package artifixal.agrest.auth;

import artifixal.agrest.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extracts PASETO access token from request cookies.
 */
@Component
@AllArgsConstructor
public class PasetoSecurityContextRepository implements ServerSecurityContextRepository {

    private final PasetoAuthenticationManager authManager;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        final HttpCookie accessTokenCookie = exchange.getRequest()
            .getCookies()
            .getFirst(UserService.ACCESS_TOKEN_COOKIE_NAME);
        if (accessTokenCookie == null)
            return Mono.empty();
        return Mono.just(accessTokenCookie)
            .flatMap((token) -> {
                var authToken = new UsernamePasswordAuthenticationToken(token, token);
                return authManager.authenticate(authToken)
                    .map((auth) -> (SecurityContext) new SecurityContextImpl(auth));
            });
    }
}
