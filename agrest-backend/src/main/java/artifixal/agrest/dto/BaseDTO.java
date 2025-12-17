package artifixal.agrest.dto;

import java.util.Optional;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Base class for DTOs.
 *
 * @param <T> ID type.
 */
@Getter
@SuperBuilder
public class BaseDTO<T> {
    private final Optional<T> id;
}
