package artifixal.agrest.deserializer;

import artifixal.agrest.dto.user.SecurePassword;
import java.nio.charset.StandardCharsets;
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
 * Unit tests of {@code SecurePasswordDeserializer}.
 */
public class SecurePasswordDeserializerUnitTest {

    @Test
    public void canDeserialize() {
        // Prepare data
        String password = "Passwordπ1234567890ą\uD83D\uDE00";
        byte[] clearArr = password.getBytes(StandardCharsets.UTF_8);
        Arrays.fill(clearArr, (byte) 0);

        // Mocks
        JsonParser parser = Mockito.mock(JsonParser.class);
        DeserializationContext ctx = Mockito.mock(DeserializationContext.class);

        when(parser.nextToken())
            .thenReturn(JsonToken.VALUE_STRING);
        when(parser.getStringCharacters()).thenReturn(password.toCharArray());
        when(parser.getStringOffset()).thenReturn(0);
        when(parser.getStringLength()).thenReturn(password.length());

        // Act
        SecurePasswordDeserializer deserializer = new SecurePasswordDeserializer();
        SecurePassword result = deserializer.deserialize(parser, ctx);

        // Asertions
        Assertions.assertArrayEquals(password.getBytes(StandardCharsets.UTF_8), result.getValue());
        result.clear();
        Assertions.assertArrayEquals(clearArr, result.getValue());

        // Verify
        verify(parser, times(1)).getStringCharacters();
    }
}
