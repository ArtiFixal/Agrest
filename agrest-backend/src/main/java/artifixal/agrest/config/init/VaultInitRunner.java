package artifixal.agrest.config.init;

import artifixal.agrest.dto.vault.SingleKeyDTO;
import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.ReactiveVaultTemplate;

/**
 * Initializes Vault with secrets.
 */
@Component
@RequiredArgsConstructor
public class VaultInitRunner {

    @Value("${spring.cloud.vault.kv.backend}")
    private String keyValPath;

    private final ReactiveVaultTemplate vaultTemplate;

    @PostConstruct
    public void initVault() throws Exception {
        var k2 = vaultTemplate.opsForVersionedKeyValue(keyValPath);
        var csrfKeyCheck = k2
            .get("agrest-app/csrf", SingleKeyDTO.class)
            .blockOptional();
        if (csrfKeyCheck.isEmpty()) {
            // CSRF key
            SecureRandom rand = SecureRandom.getInstanceStrong();
            byte[] csrfKey = new byte[32];
            rand.nextBytes(csrfKey);
            HashMap<String, String> csrfData = new HashMap<>();
            Encoder b64 = Base64.getEncoder();
            String csrfKeyStr = b64
                .encodeToString(csrfKey);
            csrfData.put("key", csrfKeyStr);
            k2.put("agrest-app/csrf", csrfData)
                .block();

            // PASETO Keypair
            KeyPair pasetoKeyPair = KeyPairGenerator.getInstance("Ed25519")
                .generateKeyPair();
            HashMap<String, String> pasetoKeys = new HashMap<>();
            String pubKeyStr = b64.encodeToString(pasetoKeyPair.getPublic()
                .getEncoded());
            pasetoKeys.put("publicKey", pubKeyStr);
            String prvKeyStr = b64.encodeToString(pasetoKeyPair.getPrivate()
                .getEncoded());
            pasetoKeys.put("privateKey", prvKeyStr);
            k2.put("agrest-app/paseto-keys", pasetoKeys)
                .block();

            // Pepper
            byte[] pepper = new byte[32];
            rand.nextBytes(pepper);
            HashMap<String, String> pepperData = new HashMap<>();
            pepperData.put("key", new String(pepper));
            k2.put("agrest-app/pepper", pepperData)
                .block();
        } else
            csrfKeyCheck.get()
                .getData()
                .key()
                .close();
    }
}
