package artifixal.agrest.deserializer;

import artifixal.agrest.dto.user.SecurePassword;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Deserialzies {@code SecurePassword} from JSON.
 */
public class SecurePasswordDeserializer extends ValueDeserializer<SecurePassword>{

    @Override
    public SecurePassword deserialize(JsonParser p,DeserializationContext ctxt) throws JacksonException{
        final CharsetEncoder encoder=StandardCharsets.UTF_8
            .newEncoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
        CharBuffer charBuff=CharBuffer.wrap(p.getStringCharacters(),
            p.getStringOffset(),p.getStringLength());
        ByteBuffer byteBuff=ByteBuffer.allocate(p.getStringLength()*4);
        encoder.encode(charBuff,byteBuff,true);
        encoder.flush(byteBuff);
        byteBuff.flip();
        int length=byteBuff.remaining();
        byte[] secret=new byte[length];
        byteBuff.get(secret);
        if(byteBuff.hasArray())
            Arrays.fill(byteBuff.array(),(byte)0);
        return new SecurePassword(secret);
    }
}
