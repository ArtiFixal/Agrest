package artifixal.agrest.dto.user;

import artifixal.agrest.dto.BaseDTO;
import jakarta.validation.constraints.Email;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * DTO class containing user information.
 */
@Getter
@SuperBuilder
public class UserDTO extends BaseDTO<UUID>{
    
    @Email
    private String email;
    private LocalDateTime expireDate;
    private int role;
    private boolean enabled;
    private boolean locked;
    private boolean forcedPasswordChange;
}
