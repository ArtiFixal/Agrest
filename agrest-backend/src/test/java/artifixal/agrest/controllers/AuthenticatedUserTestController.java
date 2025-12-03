package artifixal.agrest.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller for tesing purposes of being able to access endpoints after login.
 */
@RestController
public class AuthenticatedUserTestController {
    
    @GetMapping("/auth/accessTest")
    public Mono<String> onlyForAuthorized(){
        return Mono.just("ok");
    }
}
