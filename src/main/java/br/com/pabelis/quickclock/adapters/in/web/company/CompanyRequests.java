package br.com.pabelis.quickclock.adapters.in.web.company;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

final class CompanyRequests {

    private CompanyRequests() {
    }

    record SaveCompanyRequest(
        @NotBlank(message = "is required")
        @Size(max = 120, message = "must have at most 120 characters")
        String name
    ) {
    }

    record SaveCompanySettingsRequest(
        @Min(value = 0, message = "must be zero or greater")
        long halfDayValueCents,

        @NotNull(message = "is required")
        WeekdaySettingsRequest weekdays
    ) {
    }

    record WeekdaySettingsRequest(
        boolean mondayActive,
        boolean tuesdayActive,
        boolean wednesdayActive,
        boolean thursdayActive,
        boolean fridayActive,
        boolean saturdayActive,
        boolean sundayActive
    ) {
    }
}
