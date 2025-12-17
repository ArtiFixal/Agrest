package artifixal.agrest.deserializer;

import artifixal.agrest.dto.vault.SecureSecret;
import java.util.Arrays;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Deserializes {@code SecureSecret} from JSON.
 */
public class SecureSecretDeserializer extends ValueDeserializer<SecureSecret> {

    @Override
    public SecureSecret deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
        byte[] buff = p.getBinaryValue();
        byte[] secret = Arrays.copyOf(buff, buff.length);
        // Zero jackson buff
        Arrays.fill(buff, (byte) 0);
        return new SecureSecret(secret);
    }
}
