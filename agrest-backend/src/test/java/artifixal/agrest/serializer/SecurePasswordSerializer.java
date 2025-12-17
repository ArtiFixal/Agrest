package artifixal.agrest.serializer;

import artifixal.agrest.dto.user.SecurePassword;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Serializes {@code SecurePassword} to JSON.
 */
public class SecurePasswordSerializer extends ValueSerializer<SecurePassword> {

    @Override
    public void serialize(SecurePassword value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        if (value == null || value.getValue() == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(new String(value.getValue()));
    }
}
