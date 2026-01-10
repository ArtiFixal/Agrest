package artifixal.agrest.token.paseto;

import artifixal.agrest.auth.UserRole;
import artifixal.agrest.dto.vault.KeyPairDTO;
import artifixal.agrest.dto.vault.SecureSecret;
import artifixal.agrest.entity.User;
import artifixal.agrest.services.VaultService;
import artifixal.paseto4jutils.ParsedToken;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.vault.support.Versioned;
import reactor.core.publisher.Mono;

/**
 * Unit tests of the {@code PasetoService}.
 */
@ExtendWith(MockitoExtension.class)
public class PasetoServiceUnitTest {

    @Mock
    private VaultService vaultService;
    private PasetoService pasetoService;

    @BeforeAll
    public static void beforeAll() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @BeforeEach
    public void setUp() throws InvalidKeySpecException, NoSuchAlgorithmException {
        // Init data
        final Versioned<KeyPairDTO> keys = Versioned.create(createKeyPair());
        // Mock key fetch
        when(vaultService.getPasetoKeys()).thenReturn(Mono.just(keys));
        pasetoService = new PasetoService(vaultService);
    }

    @Test
    public void canVerifyToken() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final User user = new User("email", new byte[]{0}, UserRole.USER, LocalDateTime.MIN, true, true, true);
        final UUID userID = UUID.randomUUID();
        user.setId(userID);

        // Act
        String accessToken = pasetoService.createAccessTokenForUser(user);
        String refreshToken = pasetoService.createRefreshTokenForUser(user);
        var accessAuth = pasetoService.validateToken(accessToken)
            .block();
        var refreshAuth = pasetoService.validateToken(refreshToken)
            .block();

        // Assert
        ParsedToken accessParsedToken = (ParsedToken) accessAuth.getCredentials();
        assertEquals(userID.toString(), accessAuth.getPrincipal().toString());
        assertNotNull(accessParsedToken.getExpirationAsLocalDateTime());
        assertEquals(String.valueOf(UserRole.USER.getRoleID()), accessParsedToken.get("role"));
        assertEquals(userID.toString(), refreshAuth.getPrincipal().toString());
    }

    private KeyPairDTO createKeyPair() throws NoSuchAlgorithmException {
        final KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519")
            .generateKeyPair();
        SecureSecret pubKey = new SecureSecret(keyPair.getPublic().getEncoded());
        SecureSecret prvKey = new SecureSecret(keyPair.getPrivate().getEncoded());
        return new KeyPairDTO(pubKey, prvKey);
    }
}
