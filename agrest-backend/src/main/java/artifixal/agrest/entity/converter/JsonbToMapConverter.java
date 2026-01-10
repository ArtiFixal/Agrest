package artifixal.agrest.entity.converter;

import io.r2dbc.postgresql.codec.Json;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 *
 */
@ReadingConverter
@AllArgsConstructor
public class JsonbToMapConverter implements Converter<Json, Map<String, String>> {

    private final ObjectMapper mapper;

    @Override
    public Map convert(Json source) {
        if (source == null || source.asString().isBlank())
            return Map.of();
        return mapper.readValue(source.asString(), new TypeReference<Map<String, String>>() {
        });
    }
}
