package artifixal.agrest.filters.csrf;

import artifixal.agrest.exceptions.CsrfTokenException;
import artifixal.agrest.services.CsrfService;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * WebFilter for per-request CSRF token validation in REST API.
 */
@Component
@AllArgsConstructor
public class PerRequestCsrfWebFilter implements WebFilter {
    /**
     * Argument name indicating that CSRF was already validated.
     */
    private final static String ALREADY_VALIDATED = "CSRF_VALIDATED";

    /**
     * HTTP considered safe - methods not changing any state
     */
    private static final Set<HttpMethod> SAFE_METHODS = Set.of(
        HttpMethod.GET,
        HttpMethod.HEAD,
        HttpMethod.OPTIONS,
        HttpMethod.TRACE);

    /**
     * Endpoints not secured by CSRF tokens.
     */
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
        "/v1/auth/login",
        "/v1/auth/refresh");

    private final CsrfService csrfService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Check if CSRF was already validated
        if (exchange.getAttribute(ALREADY_VALIDATED) != null)
            return chain.filter(exchange);
        ServerHttpRequest request = exchange.getRequest();
        // Skip CSRF check if safe method or public endpoint
        if (isSafeMethod(request) || isPublicEndpoint(request))
            return chain.filter(exchange);
        String csrfHeader = request.getHeaders()
            .getFirst(CsrfService.CSRF_HEADER);
        HttpCookie csrfCookie = request.getCookies()
            .getFirst(CsrfService.CSRF_COOKIE);
        if (csrfCookie == null || csrfCookie.getValue().isBlank())
            return rejectRequest(exchange);
        if (!csrfCookie.getValue().equals(csrfHeader))
            return rejectRequest(exchange);
        return csrfService.validateToken(csrfHeader)
            .flatMap((valid) -> {
                if (!valid)
                    return rejectRequest(exchange);
                exchange.getAttributes().put(ALREADY_VALIDATED, true);
                return chain.filter(exchange);
            })
            .onErrorResume(CsrfTokenException.class, (err) -> rejectRequest(exchange));
    }

    private Mono<Void> rejectRequest(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    private boolean isSafeMethod(ServerHttpRequest request) {
        return SAFE_METHODS.contains(request.getMethod());
    }

    private boolean isPublicEndpoint(ServerHttpRequest request) {
        String path = request.getPath().value();
        return PUBLIC_ENDPOINTS.contains(path);
    }
}
