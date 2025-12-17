package artifixal.agrest.dto.user;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * DTO class for user creation.
 */
@Getter
@SuperBuilder
public class UserCreationDTO extends UserDTO {

    @Setter
    private SecurePassword password;
}
