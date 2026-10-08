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
    }

    public static WorkDay create(
        Long companyId,
        LocalDate date,
        boolean workedBeforeLunch,
        boolean workedAfterLunch,
        LocalDateTime now
    ) {
        return new WorkDay(null, companyId, date, workedBeforeLunch, workedAfterLunch, now, now);
    }

    public WorkDay update(boolean newWorkedBeforeLunch, boolean newWorkedAfterLunch, LocalDateTime now) {
        return new WorkDay(id, companyId, date, newWorkedBeforeLunch, newWorkedAfterLunch, createdAt, now);
    }

    public WorkDay toggleBeforeLunch(LocalDateTime now) {
        return update(!workedBeforeLunch, workedAfterLunch, now);
    }

    public WorkDay toggleAfterLunch(LocalDateTime now) {
        return update(workedBeforeLunch, !workedAfterLunch, now);
    }
}
