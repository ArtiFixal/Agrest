package artifixal.agrest.services;

import artifixal.agrest.auth.CookieTokenPair;
import artifixal.agrest.dto.user.UserAuthenticationDTO;
import artifixal.agrest.auth.UserRole;
import artifixal.agrest.dto.user.SecurePassword;
import artifixal.agrest.repository.UserRepository;
import artifixal.agrest.dto.user.UserCreationDTO;
import artifixal.agrest.dto.vault.SecureCharSecret;
import artifixal.agrest.entity.User;
import artifixal.agrest.exceptions.AuthenticationException;
import artifixal.agrest.token.paseto.PasetoService;
import artifixal.paseto4jutils.ParsedToken;
import com.password4j.Password;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.identityconnectors.common.ByteUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service related to user logic.
 */
@Service
@DependsOn("vaultInitRunner")
@Slf4j
@RequiredArgsConstructor
public class UserService {
    public final static String ACCESS_TOKEN_COOKIE_NAME="access_token";
    public final static String REFRESH_TOKEN_COOKIE_NAME="refresh_token";
    
    @Value("${hash.argon2.length}")
    private int HASH_BYTES;
    
    private SecureCharSecret pepper;
    
    private final UserRepository repo;
    private final PasetoService pasetoService;
    private final VaultService vaultService;
    
    @PostConstruct
    private void fetchPepper(){
        pepper=vaultService.getPepper()
            .block()
            .getData()
            .key();
    }
    
    private ResponseCookie createTokenCookie(String name,String value,String path,long maxAge){
        return ResponseCookie.from(name,value)
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .maxAge(maxAge)
            .path(path)
            .build();
    }
    
    private byte[] hashPassword(SecurePassword password){
        try(password){
            byte[] hash=Password.hash(password.getValue())
            .addPepper()
            .addRandomSalt()
            .withArgon2()
            .getResultAsBytes();
            return hash;
        }
    }
    
    private CookieTokenPair createTokenCookies(User user){
        String accessToken=pasetoService.createAccessTokenForUser(user);
        String refreshToken=pasetoService.createRefreshTokenForUser(user);
        return new CookieTokenPair(
            createTokenCookie(ACCESS_TOKEN_COOKIE_NAME,
                accessToken,
                "/",
                PasetoService.ACCESS_TOKEN_VALID_PERIOD
            ),
            createTokenCookie(REFRESH_TOKEN_COOKIE_NAME,
                refreshToken,
                "/v1/auth/refresh",
                PasetoService.REFRESH_TOKEN_VALID_PERIOD
            )
        );
    }
    
    /**
     * Verifies user credentials then creates tokens for him. Calculates hash 
     * even if user not found in database.
     * 
     * @param credentials User login data.
     * 
     * @return Refresh and access tokens.
     */
    public Mono<CookieTokenPair> login(UserAuthenticationDTO credentials){
        return repo.findByEmail(credentials.email())
            // Prevent timing attacks if no user with thiat email.
            .onErrorResume((t)->{
                byte[] randomBytes=ByteUtil.randomBytes(HASH_BYTES);
                Password.check(credentials.password().getValue(),randomBytes)
                    .withArgon2();
                throw new AuthenticationException("Invalid credentials");
            })
            // Success
            .map((user)->{
                boolean valid=Password.check(credentials.password().getValue(),user.getHash())
                    .withArgon2();
                if(!valid)
                    throw new AuthenticationException("Invalid credentials");
                log.info("User {} logged in",user.getId().toString());
                return createTokenCookies(user);
            });
    }
    
    /**
     * Refresh access token.
     * 
     * @param refreshToken Cookie containing refresh token.
     * 
     * @return Rotated tokens.
     */
    public Mono<CookieTokenPair> refreshAccessToken(HttpCookie refreshToken){
        if(refreshToken==null)
            throw new AuthenticationException("Missing refresh token");
        if(!refreshToken.getName().equals(REFRESH_TOKEN_COOKIE_NAME))
            throw new AuthenticationException("Not a refresh token");
        return pasetoService.validateToken(refreshToken.getValue())
            .map((auth)->{
                ParsedToken token=(ParsedToken)auth.getCredentials();
                return UUID.fromString(token.getSubject());
            })
            .flatMap((userID)->repo.findById(userID))
            .map((user)->createTokenCookies(user));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<User> createUser(UserCreationDTO newUserData){
        byte[] hash=hashPassword(newUserData.getPassword());
        UserRole role=UserRole.fromInt(newUserData.getRole());
        User user=new User(newUserData.getEmail(),hash,role,
            newUserData.getExpireDate(),
            newUserData.isEnabled(),newUserData.isLocked(),
            newUserData.isForcedPasswordChange());
        return repo.save(user);
    }
    
    public Mono<User> getUser(UUID userID){
        return repo.findById(userID);
    }
    
    public Mono<User> getUser(String email){
        return repo.findByEmail(email);
    }
}
