package br.com.pabelis.quickclock.domain.estimate;

import br.com.pabelis.quickclock.domain.shared.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record Estimate(
    Long id, Long companyId, LocalDate date, String description, long amountCents,
    EstimateStatus status, LocalDateTime approvedAt, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public Estimate {
        if (companyId == null || date == null || status == null) {
            throw new BusinessException("Company, date and status are required.");
        }
        if (description == null || description.isBlank() || description.trim().length() > 255) {
            throw new BusinessException("Estimate description must have 1 to 255 characters.");
        }
        if (amountCents < 0) {
            throw new BusinessException("Estimate amount must be zero or greater.");
        }
        if (status == EstimateStatus.APPROVED && approvedAt == null) {
            throw new BusinessException("Approved estimate requires approval date.");
        }
        description = description.trim();
    }

    public static Estimate create(Long companyId, LocalDate date, String description, long amount, LocalDateTime now) {
        return new Estimate(null, companyId, date, description, amount, EstimateStatus.DRAFT, null, now, now);
    }

    public Estimate update(LocalDate date, String description, long amount, LocalDateTime now) {
        requireDraft();
        return new Estimate(id, companyId, date, description, amount, status, null, createdAt, now);
    }

    public Estimate approve(LocalDateTime now) {
        if (status == EstimateStatus.APPROVED) return this;
        requireDraft();
        return new Estimate(id, companyId, date, description, amountCents, EstimateStatus.APPROVED, now, createdAt, now);
    }

    public Estimate cancel(LocalDateTime now) {
        if (status == EstimateStatus.CANCELED) return this;
        requireDraft();
        return new Estimate(id, companyId, date, description, amountCents, EstimateStatus.CANCELED, null, createdAt, now);
    }

    private void requireDraft() {
        if (status != EstimateStatus.DRAFT) {
            throw new BusinessException("Only draft estimates can be changed or canceled.");
        }
    }
}
