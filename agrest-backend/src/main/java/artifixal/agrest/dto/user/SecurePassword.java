package artifixal.agrest.dto.user;

import artifixal.agrest.deserializer.SecurePasswordDeserializer;
import artifixal.agrest.dto.vault.Eraseable;
import java.io.Closeable;
import java.util.Arrays;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Wrapper around byte array to avoid String allocation.
 */
@JsonDeserialize(using=SecurePasswordDeserializer.class)
public class SecurePassword implements AutoCloseable,Closeable,Eraseable{
    
    private byte[] value;

    public SecurePassword(byte[] value){
        this.value=value;
    }

    public byte[] getValue(){
        return value;
    }

    @Override
    public void close() {
        clear();
    }

    @Override
    public void clear(){
        Arrays.fill(value,(byte)0);
    }
}
