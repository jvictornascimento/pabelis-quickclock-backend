package br.com.pabelis.quickclock.domain.workday;

import br.com.pabelis.quickclock.domain.shared.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkDay(
    Long id,
    Long companyId,
    LocalDate date,
    boolean workedBeforeLunch,
    boolean workedAfterLunch,
    Long beforeLunchValueCents,
    Long afterLunchValueCents,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public WorkDay {
        if (companyId == null) {
            throw new BusinessException("Company id is required.");
        }
        if (date == null) {
            throw new BusinessException("Work day date is required.");
        }
        validatePrice(beforeLunchValueCents, workedBeforeLunch);
        validatePrice(afterLunchValueCents, workedAfterLunch);
    }

    private static void validatePrice(Long price, boolean marked) {
        if (price != null && price < 0) {
            throw new BusinessException("Period value must be zero or greater.");
        }
        if (marked && price == null) {
            throw new BusinessException("Marked periods require a historical value.");
        }
    }

    public static WorkDay create(
        Long companyId,
        LocalDate date,
        boolean workedBeforeLunch,
        boolean workedAfterLunch,
        long halfDayValueCents,
        LocalDateTime now
    ) {
        return new WorkDay(null, companyId, date, workedBeforeLunch, workedAfterLunch,
            workedBeforeLunch ? halfDayValueCents : null, workedAfterLunch ? halfDayValueCents : null, now, now);
    }

    public WorkDay update(boolean newWorkedBeforeLunch, boolean newWorkedAfterLunch,
                          long halfDayValueCents, LocalDateTime now) {
        return new WorkDay(id, companyId, date, newWorkedBeforeLunch, newWorkedAfterLunch,
            beforeLunchValueCents == null && newWorkedBeforeLunch ? Long.valueOf(halfDayValueCents) : beforeLunchValueCents,
            afterLunchValueCents == null && newWorkedAfterLunch ? Long.valueOf(halfDayValueCents) : afterLunchValueCents,
            createdAt, now);
    }
}
