package artifixal.agrest.auth;

import org.springframework.http.ResponseCookie;

/**
 * Holds generated token cookies for user.
 */
public record CookieTokenPair(ResponseCookie accessTokenCookie, ResponseCookie refreshTokenCookie) {

}
