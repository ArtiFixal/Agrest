package artifixal.agrest.entity.converter;

import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 *
 */
@ReadingConverter
@AllArgsConstructor
public class JsonbToJsonNodeConverter implements Converter<Json, JsonNode> {

    private final ObjectMapper mapper;

    @Override
    public JsonNode convert(Json source) {
        if (source == null || source.asString().isBlank())
            return null;
        return mapper.readValue(source.asString(), JsonNode.class);
    }
}
