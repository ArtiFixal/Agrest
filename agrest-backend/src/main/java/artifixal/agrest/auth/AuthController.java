package artifixal.agrest.auth;

import artifixal.agrest.dto.user.UserAuthenticationDTO;
import artifixal.agrest.services.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.ServerRequest;
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

    @PostMapping("/login")
    public Mono<AuthenticationResultDTO> login(@RequestBody UserAuthenticationDTO credentials, ServerHttpResponse response){
        return userService.login(credentials)
            .map((login)->{
                response.addCookie(login.cookies().accessTokenCookie());
                response.addCookie(login.cookies().refreshTokenCookie());
                return login.userData();
            });
    }
    
    @PostMapping("/refresh")
    public Mono<Void> refresh(ServerRequest request, ServerHttpResponse response){
        HttpCookie refreshToken=request.cookies()
            .getFirst(UserService.REFRESH_TOKEN_COOKIE_NAME);
        return userService.refreshAccessToken(refreshToken)
            .flatMap((token)->{
                response.addCookie(token);
                return Mono.empty();
            });
    }
}
