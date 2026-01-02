package artifixal.agrest.deserializer;

import artifixal.agrest.dto.vault.SecureCharSecret;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;

/**
 * Unit tests of {@code SecureCharSecretDeserializer}.
 */
public class SecureCharSecretDeserializerUnitTest {

    @Test
    public void canDeserialize() {
        // Prepare data
        String text = "1234567890qwerty";
        char[] clearArr = text.toCharArray();
        Arrays.fill(clearArr, '\0');

        // Mocks
        JsonParser parser = Mockito.mock(JsonParser.class);
        DeserializationContext ctx = Mockito.mock(DeserializationContext.class);

        when(parser.nextToken())
            .thenReturn(JsonToken.VALUE_STRING);
        when(parser.getStringCharacters()).thenReturn(text.toCharArray());
        when(parser.getStringOffset()).thenReturn(0);
        when(parser.getStringLength()).thenReturn(text.length());

        // Act
        SecureCharSecretDeserializer deserialzer = new SecureCharSecretDeserializer();
        SecureCharSecret result = deserialzer.deserialize(parser, ctx);

        // Assertions
        Assertions.assertArrayEquals(text.toCharArray(), result.value());
        result.clear();
        Assertions.assertArrayEquals(clearArr, result.value());

        // Verify
        verify(parser, times(1)).getStringCharacters();
    }
}
