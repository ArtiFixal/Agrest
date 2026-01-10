package artifixal.agrest.entity.converter;

import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import tools.jackson.databind.JsonNode;

/**
 * Converts {@code JsonNode} to {@code String}.
 */
@WritingConverter
public class JsonNodeToJsonbConverter implements Converter<JsonNode, Json> {

    @Override
    public Json convert(JsonNode source) {
        String jsonText = source.isEmpty() ? "" : source.toString();
        return Json.of(jsonText);
    }
}
