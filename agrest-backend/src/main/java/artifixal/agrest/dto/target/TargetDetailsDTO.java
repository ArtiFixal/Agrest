package artifixal.agrest.dto.target;

import artifixal.agrest.dto.TagDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DTO transporting detailed info about {@code Target}.
 */
public record TargetDetailsDTO(Long id, String name, String url, Optional<String> description, List<TagDTO> tags,
    Optional<Map<String, String>> headers, Optional<Map<String, String>> cookies, LocalDateTime created,
    Optional<LocalDateTime> edited) {

}
