package artifixal.agrest.deserializer;

import artifixal.agrest.dto.vault.SecureCharSecret;
import java.util.Arrays;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Deserializes {@code SecureCharSecret} from JSON.
 */
public class SecureCharSecretDeserializer extends ValueDeserializer<SecureCharSecret>{

    @Override
    public SecureCharSecret deserialize(JsonParser p,DeserializationContext ctxt) throws JacksonException{
        p.getStringCharacters();
        int to=p.getStringOffset()+p.getStringLength();
        char[] value=Arrays.copyOfRange(p.getStringCharacters(),p.getStringOffset(),to);
        return new SecureCharSecret(value);
    }
}

