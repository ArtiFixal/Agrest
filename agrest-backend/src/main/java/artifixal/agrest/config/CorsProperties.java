package artifixal.agrest.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Contains configuration properties for CORS.
 */
@Component
@ConfigurationProperties("app.security.cors")
@Getter
@Setter
public class CorsProperties {
    private List<String> allowedOrigins;

}
