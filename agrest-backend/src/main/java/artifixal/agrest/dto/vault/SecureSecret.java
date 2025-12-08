package artifixal.agrest.dto.vault;

import artifixal.agrest.deserializer.SecureSecretDeserializer;
import java.io.Closeable;
import java.util.Arrays;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Wrapper around char array to prevent String allocation.
 */
@JsonDeserialize(using=SecureSecretDeserializer.class)
public record SecureSecret(byte[] value) implements AutoCloseable,Closeable,Eraseable{

    @Override
    public void close() {
        clear();
    }

    @Override
    public void clear(){
        Arrays.fill(value,(byte)0);
    }
}
