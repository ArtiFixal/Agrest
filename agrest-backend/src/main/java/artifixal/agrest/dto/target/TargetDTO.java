package artifixal.agrest.dto.target;

import artifixal.agrest.validators.ValidURL;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DTO used in creation and update of {@code Target}.
 */
public record TargetDTO(@NotBlank String name, @ValidURL String url, Optional<String> description,
    Optional<List<@NotBlank String>> tags, Optional<Map<@NotBlank String, @NotBlank String>> headers,
    Optional<Map<@NotBlank String, @NotBlank String>> cookies) {

}
