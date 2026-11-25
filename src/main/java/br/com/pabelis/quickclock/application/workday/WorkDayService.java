package br.com.pabelis.quickclock.application.workday;

import br.com.pabelis.quickclock.application.company.port.CompanyRepositoryPort;
import br.com.pabelis.quickclock.application.company.port.CompanySettingsRepositoryPort;
import br.com.pabelis.quickclock.application.finance.port.FinanceSourceGuard;
import br.com.pabelis.quickclock.domain.company.CompanySettings;
import br.com.pabelis.quickclock.domain.company.WeekdaySettings;
import br.com.pabelis.quickclock.domain.shared.BusinessException;
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
    private final CompanySettingsRepositoryPort settingsRepository;
    private final FinanceSourceGuard financeSourceGuard;
    private final Clock clock;
    private final WorkDayEditPolicy editPolicy;

    public WorkDayService(
        WorkDayRepositoryPort workDayRepository,
        CompanyRepositoryPort companyRepository,
        CompanySettingsRepositoryPort settingsRepository,
        FinanceSourceGuard financeSourceGuard,
        Clock clock
    ) {
        this.workDayRepository = workDayRepository;
        this.companyRepository = companyRepository;
        this.settingsRepository = settingsRepository;
        this.financeSourceGuard = financeSourceGuard;
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
        workDayRepository.lockCompany(companyId);
        LocalDateTime now = now();
        CompanySettings settings = writableSettings(companyId, date, now);

        var existing = workDayRepository.findByCompanyIdAndDate(companyId, date);
        if (existing.isPresent()) {
            WorkDay workDay = existing.get();
            if (workDay.workedBeforeLunch() == workedBeforeLunch && workDay.workedAfterLunch() == workedAfterLunch) {
                return workDay;
            }
            financeSourceGuard.lockAndAssertUnreceived("WORK_REPORT", workDay.id(), companyId);
            return workDayRepository.save(workDay.update(workedBeforeLunch, workedAfterLunch,
                settings.halfDayValueCents(), now));
        }

        return workDayRepository.save(WorkDay.create(companyId, date, workedBeforeLunch, workedAfterLunch,
            settings.halfDayValueCents(), now));
    }

    @Transactional
    public WorkDay toggleBeforeLunch(Long companyId, LocalDate date) {
        return toggle(companyId, date, true);
    }

    @Transactional
    public WorkDay toggleAfterLunch(Long companyId, LocalDate date) {
        return toggle(companyId, date, false);
    }

    private WorkDay toggle(Long companyId, LocalDate date, boolean beforeLunch) {
        workDayRepository.lockCompany(companyId);
        LocalDateTime now = now();
        CompanySettings settings = writableSettings(companyId, date, now);
        WorkDay workDay = workDayRepository.findByCompanyIdAndDate(companyId, date)
            .orElseGet(() -> WorkDay.create(companyId, date, false, false, settings.halfDayValueCents(), now));
        if (workDay.id() != null) {
            financeSourceGuard.lockAndAssertUnreceived("WORK_REPORT", workDay.id(), companyId);
        }
        return workDayRepository.save(workDay.update(
            beforeLunch ? !workDay.workedBeforeLunch() : workDay.workedBeforeLunch(),
            beforeLunch ? workDay.workedAfterLunch() : !workDay.workedAfterLunch(),
            settings.halfDayValueCents(), now));
    }

    private CompanySettings writableSettings(Long companyId, LocalDate date, LocalDateTime now) {
        var company = companyRepository.findById(companyId)
            .orElseThrow(() -> new NotFoundException("Company not found."));
        if (!company.active()) {
            throw new BusinessException("Inactive companies cannot register work days.");
        }
        editPolicy.ensureCanEdit(date, now.toLocalDate());
        var settings = settingsRepository.findByCompanyId(companyId)
            .orElseThrow(() -> new NotFoundException("Company settings not found."));
        if (!isActiveWeekday(settings.weekdays(), date)) {
            throw new BusinessException("There is no work scheduled for this weekday.");
        }
        return settings;
    }

    private boolean isActiveWeekday(WeekdaySettings weekdays, LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> weekdays.mondayActive();
            case TUESDAY -> weekdays.tuesdayActive();
            case WEDNESDAY -> weekdays.wednesdayActive();
            case THURSDAY -> weekdays.thursdayActive();
            case FRIDAY -> weekdays.fridayActive();
            case SATURDAY -> weekdays.saturdayActive();
            case SUNDAY -> weekdays.sundayActive();
        };
    }

    private void ensureCompanyExists(Long companyId) {
        companyRepository.findById(companyId)
            .orElseThrow(() -> new NotFoundException("Company not found."));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
