package artifixal.agrest.dto.vault;

/**
 * DTO transporting single key from Vault.
 */
public record SingleKeyDTO(SecureSecret key) implements Eraseable{

    @Override
    public void clear(){
        key.clear();
    }
}
