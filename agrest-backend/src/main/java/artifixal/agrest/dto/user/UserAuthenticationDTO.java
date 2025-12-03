package artifixal.agrest.dto.user;

/**
 * User credentials for authentiation purposes.
 */
public record UserAuthenticationDTO(String email,SecurePassword password) {
    
}
