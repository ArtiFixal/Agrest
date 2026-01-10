package artifixal.agrest.auth;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * Interface to mock tokens in integration tests.
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockTokenSecurityContextFactory.class)
public @interface WithMockToken {

    /**
     * @return User ID. By default ID representing the system.
     */
    String userID() default "00000000-0000-0000-0000-000000000000";

    /**
     * @return Granted role.
     */
    String role() default "ROLE_USER";
}
