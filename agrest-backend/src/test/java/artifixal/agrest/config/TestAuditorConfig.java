package artifixal.agrest.config;

import artifixal.agrest.entity.AuditorProvider;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

/**
 * Auditor for testing purposes.
 */
@Configuration
@EnableR2dbcAuditing(auditorAwareRef = "auditorProvider", modifyOnCreate = false)
public class TestAuditorConfig {

    private final static UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private final static AuditorProvider auditor = new AuditorProvider();

    @Bean
    public ReactiveAuditorAware<UUID> auditorProvider() {
        // If no auth user use system ID
        return () -> auditor.getCurrentAuditor()
            .onErrorReturn(TEST_UUID);
    }
}
