package artifixal.agrest.auth;

import artifixal.agrest.dto.user.UserAuthenticationDTO;
import artifixal.agrest.services.UserService;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * REST controller related to user authentication.
 */
@Slf4j
@RequestMapping("/v1/auth")
@RestController
@AllArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
    private Function<CookieTokenPair,Mono<ServerResponse>> okWithTokenCookies(){
        return (success)->ServerResponse.ok()
            .cookie(success.accessTokenCookie())
            .cookie(success.refreshTokenCookie())
            .build();
    }

    @PostMapping("/login")
    public Mono<ServerResponse> login(@RequestBody UserAuthenticationDTO credentials){
        return userService.login(credentials)
            .flatMap(okWithTokenCookies());
    }
    
    @PostMapping("/refresh")
    public Mono<ServerResponse> refresh(ServerRequest request){
        HttpCookie refreshToken=request.cookies()
            .getFirst(UserService.REFRESH_TOKEN_COOKIE_NAME);
        return userService.refreshAccessToken(refreshToken)
            .flatMap(okWithTokenCookies());
    }
}
