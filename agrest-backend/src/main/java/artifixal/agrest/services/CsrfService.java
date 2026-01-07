package artifixal.agrest.services;

import artifixal.agrest.dto.vault.SecureSecret;
import artifixal.agrest.exceptions.CsrfTokenException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service related to generating and validating anti CSRF tokens.
 */
@Service
@DependsOn("vaultInitRunner")
@RequiredArgsConstructor
public class CsrfService {
    /**
     * HTTP header containing user submited CSRF token.
     */
    public final static String CSRF_HEADER = "X-XSRF-TOKEN";

    /**
     * HTTP cookie name containing generated CSRF token.
     */
    public final static String CSRF_COOKIE = "csrf";

    /**
     * Time after CSRF token is invalid.
     */
    @Value("${app.csrf.ttl}")
    private int CSRF_TOKEN_TTL;
    private final VaultService vaultService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

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

    private Mono<byte[]> calcHmac(String token, String userID) {
        return vaultService.getCsrfKey()
            .map((key) -> createMacFunction(key.getData().key()))
            .map((hmacFunction) -> {
                // Bind userID to crypto
                hmacFunction.update(userID.getBytes());
                return hmacFunction.doFinal(token.getBytes());
            });
    }

    /**
     * Generates per-request user binded token and saves it in Redis with TTL for later usage validation.
     *
     * @return Random CSRF token.
     * @see #CSRF_TOKEN_TTL
     */
    public Mono<String> generateToken() {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap((auth) -> {
                UUID nonce = UUID.randomUUID();
                String userID = auth.getAuthentication()
                    .getPrincipal()
                    .toString();
                String token = nonce.toString();
                return calcHmac(token, userID).map((hmac) -> {
                    return token + "." + String.valueOf(Hex.encode(hmac));
                });
            })
            .flatMap((token) -> {
                String key = "csrf:" + token;
                return redisTemplate.opsForValue()
                    .set(key, "valid", getTokenTTL())
                    .thenReturn(token);
            });
    }

    /**
     * @return Is token valid?
     */
    public Mono<Boolean> validateToken(String token) {
        if (token == null || token.isBlank())
            return Mono.error(new CsrfTokenException("Missing CSRF token"));
        String[] tokenParts = token.split("\\.");
        if (tokenParts.length != 2)
            return Mono.error(new CsrfTokenException("Malformed CSRF token"));
        return ReactiveSecurityContextHolder.getContext()
            .map((auth) -> auth.getAuthentication().getPrincipal().toString())
            .flatMap((userID) -> {
                byte[] receivedHmac = Hex.decode(tokenParts[1]);
                return calcHmac(tokenParts[0], userID)
                    .flatMap((calculatedHmac) -> {
                        if (!MessageDigest.isEqual(receivedHmac, calculatedHmac))
                            return Mono.error(new CsrfTokenException("Invalid CSRF token"));
                        String key = "csrf:" + token;
                        return redisTemplate.opsForValue()
                            .getAndDelete(key)
                            .switchIfEmpty(Mono.error(new CsrfTokenException("CSRF token already used")))
                            .map((fetchedToken) -> fetchedToken.equals("valid"));
                    });
            });
    }

    public ResponseCookie createCsrfCookie(String token) {
        return ResponseCookie.from(CSRF_COOKIE, token)
            .httpOnly(false)
            .secure(true)
            .sameSite("Strict")
            .maxAge(CSRF_TOKEN_TTL)
            .path("/")
            .build();
    }

    public Duration getTokenTTL() {
        return Duration.ofSeconds(CSRF_TOKEN_TTL);
    }
}
