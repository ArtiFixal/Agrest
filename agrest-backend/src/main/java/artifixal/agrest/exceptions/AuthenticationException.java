package artifixal.agrest.exceptions;

/**
 * Exception related to authentication errors.
 */
public class AuthenticationException extends org.springframework.security.core.AuthenticationException {

    public AuthenticationException(String msg) {
        super(msg);
    }
}
