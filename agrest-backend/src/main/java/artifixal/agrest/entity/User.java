package artifixal.agrest.entity;

import artifixal.agrest.auth.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity class representing system user.
 */
@Getter
@Setter
@Table(name = "users")
public class User extends AuditableEntity<UUID> {
    private String email;

    @JsonIgnore
    private byte[] hash;

    /**
     * Date to which account will be able to login.
     */
    private LocalDateTime expireDate;

    /**
     * Able to authenticate.
     */
    private boolean enabled;

    /**
     * Unable to attempt login due to not exceeded login attepts or
     * other reasons.
     */
    private boolean locked;

    /**
     * User system role.
     */
    private int role;

    /**
     * Does password need to be changed.
     */
    private boolean forcedPasswordChange;

    public User(String email, byte[] hash, UserRole role, LocalDateTime expireDate, boolean enabled, boolean locked,
        boolean forcedPasswordChange) {
        super(null);
        this.email = email;
        this.hash = hash;
        this.role = role.getRoleID();
        this.expireDate = expireDate;
        this.enabled = enabled;
        this.locked = locked;
        this.forcedPasswordChange = forcedPasswordChange;
    }
}
