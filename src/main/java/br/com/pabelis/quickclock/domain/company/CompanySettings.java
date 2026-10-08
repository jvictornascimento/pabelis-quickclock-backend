package br.com.pabelis.quickclock.domain.company;

import br.com.pabelis.quickclock.domain.shared.BusinessException;

import java.time.LocalDateTime;

public record CompanySettings(
    Long id,
    Long companyId,
    long halfDayValueCents,
    WeekdaySettings weekdays,
    LocalDateTime updatedAt
) {

    public CompanySettings {
        if (companyId == null) {
            throw new BusinessException("Company id is required.");
        }
        if (halfDayValueCents < 0) {
            throw new BusinessException("Half day value must be zero or greater.");
        }
        if (weekdays == null) {
            throw new BusinessException("Weekday settings are required.");
        }
    }

    public static CompanySettings defaultFor(Long companyId, LocalDateTime now) {
        return new CompanySettings(null, companyId, 0, WeekdaySettings.mondayToFriday(), now);
    }

    public CompanySettings update(long newHalfDayValueCents, WeekdaySettings newWeekdays, LocalDateTime now) {
        return new CompanySettings(id, companyId, newHalfDayValueCents, newWeekdays, now);
    }
}
