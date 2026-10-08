package br.com.pabelis.quickclock.adapters.out.persistence.company;

import br.com.pabelis.quickclock.domain.company.Company;
import br.com.pabelis.quickclock.domain.company.CompanySettings;
import br.com.pabelis.quickclock.domain.company.WeekdaySettings;

final class CompanyMapper {

    private CompanyMapper() {
    }

    static Company toDomain(CompanyEntity entity) {
        return new Company(
            entity.getId(),
            entity.getName(),
            entity.isActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    static CompanyEntity toEntity(Company company) {
        return new CompanyEntity(
            company.id(),
            company.name(),
            company.active(),
            company.createdAt(),
            company.updatedAt()
        );
    }

    static CompanySettings toDomain(CompanySettingsEntity entity) {
        return new CompanySettings(
            entity.getId(),
            entity.getCompanyId(),
            entity.getHalfDayValueCents(),
            new WeekdaySettings(
                entity.isMondayActive(),
                entity.isTuesdayActive(),
                entity.isWednesdayActive(),
                entity.isThursdayActive(),
                entity.isFridayActive(),
                entity.isSaturdayActive(),
                entity.isSundayActive()
            ),
            entity.getUpdatedAt()
        );
    }

    static CompanySettingsEntity toEntity(CompanySettings settings) {
        WeekdaySettings weekdays = settings.weekdays();
        return new CompanySettingsEntity(
            settings.id(),
            settings.companyId(),
            settings.halfDayValueCents(),
            weekdays.mondayActive(),
            weekdays.tuesdayActive(),
            weekdays.wednesdayActive(),
            weekdays.thursdayActive(),
            weekdays.fridayActive(),
            weekdays.saturdayActive(),
            weekdays.sundayActive(),
            settings.updatedAt()
        );
    }
}
