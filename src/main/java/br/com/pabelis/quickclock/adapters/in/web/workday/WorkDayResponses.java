package br.com.pabelis.quickclock.adapters.in.web.workday;

import br.com.pabelis.quickclock.domain.workday.WorkDay;

import java.time.LocalDate;
import java.time.LocalDateTime;

final class WorkDayResponses {

    private WorkDayResponses() {
    }

    record WorkDayResponse(
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

        static WorkDayResponse from(WorkDay workDay) {
            return new WorkDayResponse(
                workDay.id(),
                workDay.companyId(),
                workDay.date(),
                workDay.workedBeforeLunch(),
                workDay.workedAfterLunch(),
                workDay.beforeLunchValueCents(),
                workDay.afterLunchValueCents(),
                workDay.createdAt(),
                workDay.updatedAt()
            );
        }
    }
}
