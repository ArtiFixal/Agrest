package artifixal.agrest.dto.vault;

import artifixal.agrest.deserializer.SecureCharSecretDeserializer;
import java.io.Closeable;
import java.util.Arrays;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Wrapper around char array to avoid String allocation.
 */
@JsonDeserialize(using = SecureCharSecretDeserializer.class)
public record SecureCharSecret(char[] value) implements AutoCloseable, Closeable, Eraseable, CharSequence {

    @Override
    public void close() {
        clear();
    }

    @Override
    public void clear() {
        Arrays.fill(value, '\0');
    }

    @Override
    public int length() {
        return value.length;
    }

    @Override
    public char charAt(int index) {
        return value[index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new SecureCharSecret(Arrays.copyOfRange(value, start, end));
    }
}
