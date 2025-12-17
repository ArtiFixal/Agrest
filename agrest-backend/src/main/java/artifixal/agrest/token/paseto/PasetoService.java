package artifixal.agrest.token.paseto;

import artifixal.agrest.auth.UserRole;
import artifixal.agrest.dto.vault.KeyPairDTO;
import artifixal.agrest.entity.User;
import artifixal.agrest.services.VaultService;
import artifixal.paseto4jutils.ParsedToken;
import artifixal.paseto4jutils.PasetoBuilder;
import artifixal.paseto4jutils.PasetoParser;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.NamedParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Collections;
import org.apache.commons.lang3.ArrayUtils;
import org.paseto4j.commons.PasetoException;
import org.paseto4j.commons.PrivateKey;
import org.paseto4j.commons.PublicKey;
import org.paseto4j.commons.Version;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service related to issuing and validating PASETO tokens.
 */
@Service
@DependsOn("vaultInitRunner")
public class PasetoService {
    /**
     * How long in seconds is access token valid.
     */
    public final static int ACCESS_TOKEN_VALID_PERIOD = 30 * 60;

    /**
     * How long in seconds is refresh token valid.
     */
    public final static int REFRESH_TOKEN_VALID_PERIOD = 7 * 24 * 3600;

    private final VaultService vaultService;
    private PrivateKey signingKey;
    private PublicKey verifyKey;

    public PasetoService(VaultService vaultService) throws InvalidKeySpecException, NoSuchAlgorithmException {
        this.vaultService = vaultService;
        KeyPair pasetoKeys = getKeyPair();
        signingKey = new PrivateKey(pasetoKeys.getPrivate(), Version.V4);
        verifyKey = new PublicKey(pasetoKeys.getPublic(), Version.V4);
    }

    public String createAccessTokenForUser(User user) {
        final LocalDateTime issued = LocalDateTime.now();
        return PasetoBuilder.Public(signingKey, Version.V4)
            .setIssuedAt(issued)
            .setExpiration(issued.plusSeconds(ACCESS_TOKEN_VALID_PERIOD))
            .setSubject(user.getId().toString())
            .set("role", String.valueOf(user.getRole()))
            .build();
    }

    public String createRefreshTokenForUser(User user) {
        final LocalDateTime issued = LocalDateTime.now();
        return PasetoBuilder.Public(signingKey, Version.V4)
            .setIssuedAt(issued)
            .setExpiration(issued.plusSeconds(REFRESH_TOKEN_VALID_PERIOD))
            .setSubject(user.getId().toString())
            .set("role", String.valueOf(user.getRole()))
            .build();
    }

    public Mono<Authentication> validateToken(String stringToken) {
        final ParsedToken token = PasetoParser.Public(verifyKey)
            .parse(stringToken);
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(token.getExpirationAsLocalDateTime()))
            throw new PasetoException("Token expired");
        SimpleGrantedAuthority role;
        try {
            int roleValue = Integer.parseInt(token.get("role"));
            role = UserRole.fromInt(roleValue)
                .toAuthority();
        } catch (NumberFormatException e) {
            throw new PasetoException("Malformed token");
        }
        return Mono
            .just(new UsernamePasswordAuthenticationToken(token.getSubject(), token, Collections.singleton(role)));
    }

    private KeyPair getKeyPair() throws InvalidKeySpecException, NoSuchAlgorithmException {
        KeyPairDTO keys = vaultService.getPasetoKeys()
            .block()
            .getData();
        var pvSpec = new PKCS8EncodedKeySpec(keys.privateKey().value());
        var pubSpec = getPublicKey(keys.privateKey().value());
        KeyFactory keyAlg = KeyFactory.getInstance("Ed25519");
        var pvKey = keyAlg.generatePrivate(pvSpec);
        var pubKey = keyAlg.generatePublic(pubSpec);
        return new KeyPair(pubKey, pvKey);
    }

    private KeySpec getPublicKey(byte[] rawKey) {
        byte lastByte = rawKey[rawKey.length - 1];
        boolean xOdd = (lastByte & 0x80) != 0;
        byte[] yBytes = rawKey.clone();
        ArrayUtils.reverse(yBytes);
        BigInteger y = new BigInteger(1, yBytes);
        return new EdECPublicKeySpec(NamedParameterSpec.ED25519, new EdECPoint(xOdd, y));
    }
}
