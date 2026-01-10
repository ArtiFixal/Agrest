package artifixal.agrest.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 *  Contains configuration properties for pagination.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties("app.pagination")
public class PaginationProperties {
    private List<Integer> sizes;
}
