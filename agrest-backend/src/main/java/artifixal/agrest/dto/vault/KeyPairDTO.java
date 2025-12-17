package artifixal.agrest.dto.vault;

/**
 * DTO transporting keypair from Vault.
 */
public record KeyPairDTO(SecureSecret publicKey, SecureSecret privateKey) implements Eraseable {

    @Override
    public void clear() {
        privateKey.clear();
        publicKey.clear();
    }
}
