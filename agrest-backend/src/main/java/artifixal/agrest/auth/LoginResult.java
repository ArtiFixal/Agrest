package artifixal.agrest.auth;

/**
 * Represents successful login attempt.
 */
public record LoginResult(CookieTokenPair cookies,AuthenticationResultDTO userData) {

}
