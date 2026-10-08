package br.com.pabelis.quickclock.application.workday.port;

import br.com.pabelis.quickclock.domain.workday.WorkDay;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkDayRepositoryPort {

    WorkDay save(WorkDay workDay);

    Optional<WorkDay> findByCompanyIdAndDate(Long companyId, LocalDate date);

    List<WorkDay> findByCompanyIdAndDateBetween(Long companyId, LocalDate startDate, LocalDate endDate);
}
