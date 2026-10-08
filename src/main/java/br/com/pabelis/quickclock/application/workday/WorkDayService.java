package br.com.pabelis.quickclock.application.workday;

import br.com.pabelis.quickclock.application.company.port.CompanyRepositoryPort;
import br.com.pabelis.quickclock.application.workday.port.WorkDayRepositoryPort;
import br.com.pabelis.quickclock.domain.shared.NotFoundException;
import br.com.pabelis.quickclock.domain.workday.WorkDay;
import br.com.pabelis.quickclock.domain.workday.WorkDayEditPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class WorkDayService {

    private final WorkDayRepositoryPort workDayRepository;
    private final CompanyRepositoryPort companyRepository;
    private final Clock clock;
    private final WorkDayEditPolicy editPolicy;

    public WorkDayService(
        WorkDayRepositoryPort workDayRepository,
        CompanyRepositoryPort companyRepository,
        Clock clock
    ) {
        this.workDayRepository = workDayRepository;
        this.companyRepository = companyRepository;
        this.clock = clock;
        this.editPolicy = new WorkDayEditPolicy();
    }

    public WorkDay get(Long companyId, LocalDate date) {
        ensureCompanyExists(companyId);
        return workDayRepository.findByCompanyIdAndDate(companyId, date)
            .orElseThrow(() -> new NotFoundException("Work day not found."));
    }

    public List<WorkDay> searchByDate(Long companyId, LocalDate date) {
        ensureCompanyExists(companyId);
        return workDayRepository.findByCompanyIdAndDate(companyId, date)
            .map(List::of)
            .orElseGet(List::of);
    }

    public List<WorkDay> searchByMonth(Long companyId, YearMonth month) {
        ensureCompanyExists(companyId);
        return workDayRepository.findByCompanyIdAndDateBetween(
            companyId,
            month.atDay(1),
            month.atEndOfMonth()
        );
    }

    @Transactional
    public WorkDay save(Long companyId, LocalDate date, boolean workedBeforeLunch, boolean workedAfterLunch) {
        ensureCompanyExists(companyId);
        ensureCanEdit(date);
        LocalDateTime now = now();

        WorkDay workDay = workDayRepository.findByCompanyIdAndDate(companyId, date)
            .map(existing -> existing.update(workedBeforeLunch, workedAfterLunch, now))
            .orElseGet(() -> WorkDay.create(companyId, date, workedBeforeLunch, workedAfterLunch, now));

        return workDayRepository.save(workDay);
    }

    @Transactional
    public WorkDay toggleBeforeLunch(Long companyId, LocalDate date) {
        WorkDay workDay = getEditableOrCreate(companyId, date);
        return workDayRepository.save(workDay.toggleBeforeLunch(now()));
    }

    @Transactional
    public WorkDay toggleAfterLunch(Long companyId, LocalDate date) {
        WorkDay workDay = getEditableOrCreate(companyId, date);
        return workDayRepository.save(workDay.toggleAfterLunch(now()));
    }

    private WorkDay getEditableOrCreate(Long companyId, LocalDate date) {
        ensureCompanyExists(companyId);
        ensureCanEdit(date);
        LocalDateTime now = now();
        return workDayRepository.findByCompanyIdAndDate(companyId, date)
            .orElseGet(() -> WorkDay.create(companyId, date, false, false, now));
    }

    private void ensureCompanyExists(Long companyId) {
        companyRepository.findById(companyId)
            .orElseThrow(() -> new NotFoundException("Company not found."));
    }

    private void ensureCanEdit(LocalDate date) {
        editPolicy.ensureCanEdit(date, LocalDate.now(clock));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
