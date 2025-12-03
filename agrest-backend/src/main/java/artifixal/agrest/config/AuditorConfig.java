package artifixal.agrest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

/**
 * R2DBC auditing config.
 */
@Profile("!test")
@Configuration
@EnableR2dbcAuditing(modifyOnCreate=false)
public class AuditorConfig {

}
