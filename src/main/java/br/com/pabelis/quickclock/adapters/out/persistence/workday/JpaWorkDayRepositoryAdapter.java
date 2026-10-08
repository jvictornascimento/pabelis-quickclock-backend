package br.com.pabelis.quickclock.adapters.out.persistence.workday;

import br.com.pabelis.quickclock.application.workday.port.WorkDayRepositoryPort;
import br.com.pabelis.quickclock.domain.workday.WorkDay;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
class JpaWorkDayRepositoryAdapter implements WorkDayRepositoryPort {

    private final SpringDataWorkDayRepository repository;

    JpaWorkDayRepositoryAdapter(SpringDataWorkDayRepository repository) {
        this.repository = repository;
    }

    @Override
    public WorkDay save(WorkDay workDay) {
        return WorkDayMapper.toDomain(repository.save(WorkDayMapper.toEntity(workDay)));
    }

    @Override
    public Optional<WorkDay> findByCompanyIdAndDate(Long companyId, LocalDate date) {
        return repository.findByCompanyIdAndDate(companyId, date)
            .map(WorkDayMapper::toDomain);
    }

    @Override
    public List<WorkDay> findByCompanyIdAndDateBetween(Long companyId, LocalDate startDate, LocalDate endDate) {
        return repository.findByCompanyIdAndDateBetweenOrderByDateAsc(companyId, startDate, endDate).stream()
            .map(WorkDayMapper::toDomain)
            .toList();
    }
}
