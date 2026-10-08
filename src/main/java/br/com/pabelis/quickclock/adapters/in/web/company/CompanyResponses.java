package br.com.pabelis.quickclock.adapters.in.web.company;

import br.com.pabelis.quickclock.domain.company.Company;
import br.com.pabelis.quickclock.domain.company.CompanySettings;
import br.com.pabelis.quickclock.domain.company.WeekdaySettings;

import java.time.LocalDateTime;

final class CompanyResponses {

    private CompanyResponses() {
    }

    record CompanyResponse(
        Long id,
        String name,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {

        static CompanyResponse from(Company company) {
            return new CompanyResponse(
                company.id(),
                company.name(),
                company.active(),
                company.createdAt(),
                company.updatedAt()
            );
        }
    }

    record CompanySettingsResponse(
        Long id,
        Long companyId,
        long halfDayValueCents,
        WeekdaySettingsResponse weekdays,
        LocalDateTime updatedAt
    ) {

        static CompanySettingsResponse from(CompanySettings settings) {
            return new CompanySettingsResponse(
                settings.id(),
                settings.companyId(),
                settings.halfDayValueCents(),
                WeekdaySettingsResponse.from(settings.weekdays()),
                settings.updatedAt()
            );
        }
    }

    record WeekdaySettingsResponse(
        boolean mondayActive,
        boolean tuesdayActive,
        boolean wednesdayActive,
        boolean thursdayActive,
        boolean fridayActive,
        boolean saturdayActive,
        boolean sundayActive
    ) {

        static WeekdaySettingsResponse from(WeekdaySettings weekdays) {
            return new WeekdaySettingsResponse(
                weekdays.mondayActive(),
                weekdays.tuesdayActive(),
                weekdays.wednesdayActive(),
                weekdays.thursdayActive(),
                weekdays.fridayActive(),
                weekdays.saturdayActive(),
                weekdays.sundayActive()
            );
        }
    }
}
