package br.com.pabelis.quickclock.adapters.out.persistence.workday;

import br.com.pabelis.quickclock.domain.workday.WorkDay;

final class WorkDayMapper {

    private WorkDayMapper() {
    }

    static WorkDay toDomain(WorkDayEntity entity) {
        return new WorkDay(
            entity.getId(),
            entity.getCompanyId(),
            entity.getDate(),
            entity.isWorkedBeforeLunch(),
            entity.isWorkedAfterLunch(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    static WorkDayEntity toEntity(WorkDay workDay) {
        return new WorkDayEntity(
            workDay.id(),
            workDay.companyId(),
            workDay.date(),
            workDay.workedBeforeLunch(),
            workDay.workedAfterLunch(),
            workDay.createdAt(),
            workDay.updatedAt()
        );
    }
}
