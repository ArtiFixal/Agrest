package artifixal.agrest.entity;

import artifixal.agrest.exceptions.AuthenticationException;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Auditor provider for {@class AuditableEntities}.
 */
@Profile("!test")
@Component
public class AuditorProvider implements ReactiveAuditorAware<UUID> {

    @Override
    public Mono<UUID> getCurrentAuditor() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(auth -> {
                return (UUID) auth.getPrincipal();
            })
            .switchIfEmpty(Mono.error(() -> new AuthenticationException("Can't audite with unauthenticated user")));
    }
}
