package artifixal.agrest.auth;

import artifixal.agrest.dto.user.UserAuthenticationDTO;
import artifixal.agrest.services.UserService;
import artifixal.agrest.common.IntegrationTest;
import artifixal.agrest.dto.user.SecurePassword;
import artifixal.agrest.dto.user.UserCreationDTO;
import artifixal.agrest.entity.User;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Tests user authentication at /v1/auth
 */
public class AuthControllerIntegrationTest extends IntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    public void successfulLogin() {
        // Create user
        final String email = "testuser@test.localhost";
        final String password = "!123drowssaP";

        createUser(email, password);

        // Login attempt
        var response = login(email, password);
        assertEquals(email, response.userData().email());

        // Access authorized resource attempt
        get("/auth/accessTest", HttpStatus.OK)
            .addCookie(UserService.ACCESS_TOKEN_COOKIE_NAME, response.cookies().accessTokenCookie().getValue())
            .test();
    }

    @Test
    public void shouldNotLoginWrongCredentials() {
        final String email = "wrongpassworrd@test.localhost";
        createUser(email, "Password");

        var credentials = new UserAuthenticationDTO(email, new SecurePassword("1234567890".getBytes()));
        post("/v1/auth/login", HttpStatus.UNAUTHORIZED)
            .test(credentials);
    }

    @Test
    public void shouldNotLoginNonExistentUser() {
        final String email = "nouser@test.localhost";

        var credentials = new UserAuthenticationDTO(email, new SecurePassword("nouserpassword".getBytes()));
        post("/v1/auth/login", HttpStatus.UNAUTHORIZED)
            .test(credentials);
    }

    private User createUser(String email, String password) {
        var user = UserCreationDTO.builder()
            .email(email)
            .password(new SecurePassword(password.getBytes()))
            .role(UserRole.USER.getRoleID())
            .enabled(true)
            .expireDate(LocalDateTime.MAX)
            .locked(false)
            .forcedPasswordChange(false)
            .build();

        // Write user as system
        return doAs(userService.createUser(user), UserRole.ADMIN)
            .block();
    }
}
