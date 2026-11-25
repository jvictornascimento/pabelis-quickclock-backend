package br.com.pabelis.quickclock.adapters.in.web.estimate;

import br.com.pabelis.quickclock.domain.estimate.Estimate;
import br.com.pabelis.quickclock.domain.estimate.EstimateStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EstimateResponse(Long id, Long companyId, LocalDate date, String description,
    long amountCents, EstimateStatus status, LocalDateTime approvedAt) {
    public static EstimateResponse from(Estimate estimate) {
        return new EstimateResponse(estimate.id(), estimate.companyId(), estimate.date(), estimate.description(),
            estimate.amountCents(), estimate.status(), estimate.approvedAt());
    }
}
