package artifixal.agrest.dto;

import java.util.Optional;

/**
 * DTO transporting {@code Tag} data.
 */
public record TagDTO(Optional<Long> id, String name) {

    public TagDTO(String name) {
        this(Optional.empty(), name);
    }
}
