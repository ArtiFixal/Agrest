package artifixal.agrest.auth;

import artifixal.agrest.token.paseto.PasetoService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Validates PASETO token received from context.
 */
@Component
@AllArgsConstructor
public class PasetoAuthenticationManager implements ReactiveAuthenticationManager {

    private final PasetoService pasetoService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        return pasetoService.validateToken(token);
    }
}
