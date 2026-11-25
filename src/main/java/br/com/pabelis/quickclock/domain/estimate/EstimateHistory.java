package br.com.pabelis.quickclock.domain.estimate;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EstimateHistory(
    Long id, Long estimateId, EstimateStatus previousStatus, EstimateStatus newStatus, String note, String action,
    LocalDate date, String description, Long amountCents, LocalDateTime approvedAt, LocalDateTime createdAt
) {
    public static EstimateHistory snapshot(Estimate estimate, EstimateStatus previousStatus, String action) {
        return new EstimateHistory(null, estimate.id(), previousStatus, estimate.status(), action, action,
            estimate.date(), estimate.description(), estimate.amountCents(), estimate.approvedAt(), estimate.updatedAt());
    }
}
