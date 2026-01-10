package artifixal.agrest.auth;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * Factory to create test security context with mocked user identified by token.
 */
public class WithMockTokenSecurityContextFactory implements WithSecurityContextFactory<WithMockToken> {

    @Override
    public SecurityContext createSecurityContext(WithMockToken annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        SimpleGrantedAuthority role = new SimpleGrantedAuthority(annotation.role());
        Authentication auth = new UsernamePasswordAuthenticationToken(annotation.userID(), null, List.of(role));
        context.setAuthentication(auth);
        return context;
    }
}
