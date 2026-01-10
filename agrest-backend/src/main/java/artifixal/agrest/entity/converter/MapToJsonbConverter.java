package artifixal.agrest.entity.converter;

import io.r2dbc.postgresql.codec.Json;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import tools.jackson.databind.ObjectMapper;

/**
 * Converts {@code Map} to {@code String}. If map contains custom datatypes additional {@code ValueSerializer}
 * may be required.
 */
@WritingConverter
@AllArgsConstructor
public class MapToJsonbConverter implements Converter<Map<String, String>, Json> {

    private final ObjectMapper mapper;

    @Override
    public Json convert(Map source) {
        String jsonText = (source == null) ? "{}" : mapper.writeValueAsString(source);
        return Json.of(jsonText);
    }
}
