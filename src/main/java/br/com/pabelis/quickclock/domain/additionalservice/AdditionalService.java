package br.com.pabelis.quickclock.domain.additionalservice;

import br.com.pabelis.quickclock.domain.shared.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdditionalService(
    Long id,
    Long companyId,
    LocalDate date,
    String description,
    long amountCents,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public AdditionalService {
        if (companyId == null) {
            throw new BusinessException("Company id is required.");
        }
        if (date == null) {
            throw new BusinessException("Service date is required.");
        }
        if (description == null || description.isBlank()) {
            throw new BusinessException("Service description is required.");
        }
        if (description.trim().length() > 255) {
            throw new BusinessException("Service description must have at most 255 characters.");
        }
        if (amountCents < 0) {
            throw new BusinessException("Service amount must be zero or greater.");
        }
        description = description.trim();
    }

    public static AdditionalService create(
        Long companyId,
        LocalDate date,
        String description,
        long amountCents,
        LocalDateTime now
    ) {
        return new AdditionalService(null, companyId, date, description, amountCents, now, now);
    }

    public AdditionalService update(LocalDate newDate, String newDescription, long newAmountCents, LocalDateTime now) {
        return new AdditionalService(id, companyId, newDate, newDescription, newAmountCents, createdAt, now);
    }
}
