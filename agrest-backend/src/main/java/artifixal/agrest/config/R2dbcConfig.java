package artifixal.agrest.config;

import artifixal.agrest.entity.converter.JsonNodeToJsonbConverter;
import artifixal.agrest.entity.converter.MapToJsonbConverter;
import artifixal.agrest.entity.converter.JsonbToJsonNodeConverter;
import artifixal.agrest.entity.converter.JsonbToMapConverter;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import tools.jackson.databind.ObjectMapper;

/**
 * R2DBC config.
 */
@Configuration
@AllArgsConstructor
public class R2dbcConfig {

    private final ObjectMapper mapper;

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        ArrayList converters = new ArrayList();
        converters.addAll(PostgresDialect.INSTANCE.getConverters());

        converters.add(new MapToJsonbConverter(mapper));
        converters.add(new JsonbToMapConverter(mapper));
        converters.add(new JsonNodeToJsonbConverter());
        converters.add(new JsonbToJsonNodeConverter(mapper));
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, converters);
    }
}
