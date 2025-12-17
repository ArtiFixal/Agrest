package artifixal.agrest.services;

import artifixal.agrest.dto.vault.SecureSecret;
import artifixal.agrest.exceptions.CsrfTokenException;
import java.security.MessageDigest;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.AllArgsConstructor;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service related to generating and validating anti CSRF tokens.
 */
@Service
@DependsOn("vaultInitRunner")
@AllArgsConstructor
public class CsrfService {

    private final VaultService vaultService;

    private Mac createMacFunction(SecureSecret key) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(key.value(), "HmacSHA256");
            hmacSha256.init(keySpec);
            return hmacSha256;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Mac Function");
        }
    }

    private Mono<byte[]> calcHmac(String token) {
        return vaultService.getCsrfKey()
            .map((key) -> createMacFunction(key.getData().key()))
            .map((hmacFunction) -> {
                return hmacFunction.doFinal(token.getBytes());
            });
    }

    /**
     * @return Random CSRF token.
     */
    public Mono<String> generateToken() {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap((auth) -> {
                UUID nonce = UUID.randomUUID();
                String token = nonce.toString() + "!" + auth.getAuthentication()
                    .getPrincipal();
                return calcHmac(token).map((hmac) -> {
                    return token + "." + String.valueOf(hmac);
                });
            });
    }

    /**
     * @return Is token valid.
     */
    public Mono<Boolean> validateToken(String token) {
        String[] tokenParts = token.split("\\.");
        if (tokenParts.length != 2)
            throw new CsrfTokenException("Invalid CSRF token");
        String[] tokenData = tokenParts[0].split("\\!");
        return ReactiveSecurityContextHolder.getContext()
            .map((auth) -> auth.getAuthentication().getPrincipal().toString())
            .flatMap((user) -> {
                if (!user.equalsIgnoreCase(tokenData[1]))
                    throw new CsrfTokenException("Token not issued for this principal");
                byte[] receivedHmac = Hex.decode(tokenParts[1]);
                return calcHmac(tokenParts[0])
                    .map((calculatedHmac) -> MessageDigest.isEqual(receivedHmac, calculatedHmac));
            });
    }
}
