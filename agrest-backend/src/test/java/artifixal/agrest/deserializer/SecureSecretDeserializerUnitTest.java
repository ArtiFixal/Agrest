package artifixal.agrest.deserializer;

import artifixal.agrest.dto.vault.SecureSecret;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;

/**
 * Unit tests of {@code SecureSecretDeserializer}.
 */
public class SecureSecretDeserializerUnitTest {

    @Test
    public void canDeserialize() throws NoSuchAlgorithmException {
        // Prepare data
        SecureRandom rand = SecureRandom.getInstanceStrong();
        final byte[] bytes = new byte[32];
        rand.nextBytes(bytes);
        byte[] returnBytes = Arrays.copyOf(bytes, bytes.length);
        byte[] clearArr = new byte[bytes.length];
        Arrays.fill(clearArr, (byte) 0);

        // Mocks
        JsonParser parser = Mockito.mock(JsonParser.class);
        DeserializationContext ctx = Mockito.mock(DeserializationContext.class);

        when(parser.nextToken())
            .thenReturn(JsonToken.VALUE_STRING);
        when(parser.getBinaryValue()).thenReturn(returnBytes);

        // Act
        SecureSecretDeserializer deserializer = new SecureSecretDeserializer();
        SecureSecret result = deserializer.deserialize(parser, ctx);

        // Asertions
        assertArrayEquals(bytes, result.value());
        result.clear();
        assertArrayEquals(clearArr, result.value());

        // Verify
        verify(parser, times(1)).getBinaryValue();
    }
}
