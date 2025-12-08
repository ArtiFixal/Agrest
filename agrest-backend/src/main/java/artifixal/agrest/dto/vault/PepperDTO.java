package artifixal.agrest.dto.vault;

/**
 * DTO transporting pepper from Vault.
 */
public record PepperDTO(SecureCharSecret key) implements Eraseable{

    @Override
    public void clear(){
        key.clear();
    }
}
