package artifixal.agrest.auth;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Roles defining user permissions.
 */
public enum UserRole {
    ANALYST(0), USER(1), ADMIN(2);

    private final int roleID;

    private UserRole(int value) {
        roleID = value;
    }

    public int getRoleID() {
        return roleID;
    }

    @Override
    public String toString() {
        return switch (this) {
            case ANALYST -> "ROLE_ANALYST";
            case USER -> "ROLE_USER";
            case ADMIN -> "ROLE_ADMIN";
            default ->
                throw new IllegalArgumentException("Unknown UserRole enum value");
        };
    }

    public SimpleGrantedAuthority toAuthority() {
        return new SimpleGrantedAuthority(toString());
    }

    public final static UserRole fromInt(int role) {
        return switch (role) {
            case 0 -> ANALYST;
            case 1 -> USER;
            case 2 -> ADMIN;
            default ->
                throw new IllegalArgumentException("Unknown UserRole value");
        };
    }
}
