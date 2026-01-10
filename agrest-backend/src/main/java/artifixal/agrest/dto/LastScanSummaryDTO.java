package artifixal.agrest.dto;

import java.time.LocalDateTime;

/**
 * DTO transporting summary of single {@code Target} last scan.
 */
public record LastScanSummaryDTO(LocalDateTime lastScan, int info, int low, int medium, int high, int critical) {

}
