package artifixal.agrest.dto;

import java.util.List;
import java.util.Optional;

/**
 * DTO list entry with information about single {@code Target}.
 */
public record TargetEntryDTO(long id, String name, String url, List<TagDTO> tags,
    Optional<LastScanSummaryDTO> lastScan) {

}
