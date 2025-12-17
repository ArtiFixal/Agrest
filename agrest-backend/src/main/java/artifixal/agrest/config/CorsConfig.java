package artifixal.agrest.config;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * App CORS config.
 */
@Configuration
public class CorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter(CorsProperties corsProperties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(corsProperties.getAllowedOrigins());
        cors.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE"));
        cors.setAllowedHeaders(Arrays.asList(HttpHeaders.CONTENT_TYPE, "X-XSRF-TOKEN"));
        cors.setExposedHeaders(Arrays.asList("X-XSRF-TOKEN"));
        cors.setAllowCredentials(Boolean.TRUE);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return new CorsWebFilter(source);
    }
}
