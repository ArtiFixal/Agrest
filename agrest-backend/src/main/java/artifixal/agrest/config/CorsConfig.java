package artifixal.agrest.config;

import artifixal.agrest.services.CsrfService;
import artifixal.agrest.services.PageService;
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
        cors.setAllowedHeaders(Arrays.asList(HttpHeaders.CONTENT_TYPE, CsrfService.CSRF_HEADER, "X-REQUESTED-WITH",
            HttpHeaders.CACHE_CONTROL, HttpHeaders.CONNECTION, PageService.CURRENT_PAGE, PageService.PAGE_SIZE,
            PageService.TOTAL_COUNT, PageService.TOTAL_PAGES));
        cors.setExposedHeaders(Arrays.asList("X-XSRF-TOKEN", PageService.CURRENT_PAGE, PageService.PAGE_SIZE,
            PageService.TOTAL_COUNT, PageService.TOTAL_PAGES));
        cors.setAllowCredentials(Boolean.TRUE);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return new CorsWebFilter(source);
    }
}
