package artifixal.agrest.config;

import artifixal.agrest.auth.PasetoAuthenticationManager;
import artifixal.agrest.auth.PasetoSecurityContextRepository;
import artifixal.agrest.filters.csrf.PerRequestCsrfWebFilter;
import jakarta.annotation.PostConstruct;
import java.security.Security;
import lombok.AllArgsConstructor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;

/**
 * Spring Security configuration.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final PasetoAuthenticationManager authManager;
    private final PasetoSecurityContextRepository contextRepo;
    private final PerRequestCsrfWebFilter csrfFilter;

    @Bean
    public SecurityWebFilterChain webFilterChain(ServerHttpSecurity http) {
        return http
            .redirectToHttps(Customizer.withDefaults())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            // Disable Spring default csrf filter
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            // CSRF tokens are user binded we need auth context
            .addFilterAfter(csrfFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .cors((cors) -> {})
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .authenticationManager(authManager)
            .securityContextRepository(contextRepo)
            .exceptionHandling((ex) -> {
                ex.authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED));
                ex.accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN));
            })
            .authorizeExchange((exchange) -> {
                exchange.pathMatchers(HttpMethod.OPTIONS)
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, "/v1/auth/login", "/v1/auth/refresh")
                    .permitAll()
                    .anyExchange()
                    .authenticated();
            })
            .build();
    }

    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}
