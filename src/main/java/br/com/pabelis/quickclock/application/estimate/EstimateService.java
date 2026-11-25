package br.com.pabelis.quickclock.application.estimate;

import br.com.pabelis.quickclock.application.company.port.CompanyRepositoryPort;
import br.com.pabelis.quickclock.application.estimate.port.EstimateRepositoryPort;
import br.com.pabelis.quickclock.domain.estimate.Estimate;
import br.com.pabelis.quickclock.domain.estimate.EstimateHistory;
import br.com.pabelis.quickclock.domain.estimate.EstimateStatus;
import br.com.pabelis.quickclock.domain.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EstimateService {
    private final EstimateRepositoryPort estimates;
    private final CompanyRepositoryPort companies;
    private final Clock clock;

    public EstimateService(EstimateRepositoryPort estimates, CompanyRepositoryPort companies, Clock clock) {
        this.estimates = estimates;
        this.companies = companies;
        this.clock = clock;
    }

    @Transactional
    public Estimate create(Long companyId, LocalDate date, String description, long amount) {
        ensureCompany(companyId);
        Estimate saved = estimates.save(Estimate.create(companyId, date, description, amount, now()));
        estimates.appendHistory(EstimateHistory.snapshot(saved, null, "CREATED"));
        return saved;
    }

    public List<Estimate> list(Long companyId, YearMonth month) {
        ensureCompany(companyId);
        return month == null ? estimates.findByCompanyId(companyId)
            : estimates.findByCompanyIdAndDateBetween(companyId, month.atDay(1), month.atEndOfMonth());
    }

    public Estimate get(Long companyId, Long id) {
        ensureCompany(companyId);
        return estimates.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new NotFoundException("Estimate not found."));
    }

    public List<EstimateHistory> history(Long companyId, Long id) {
        get(companyId, id);
        return estimates.findHistory(id);
    }

    @Transactional
    public Estimate update(Long companyId, Long id, LocalDate date, String description, long amount) {
        Estimate previous = locked(companyId, id);
        return saveChange(previous, previous.update(date, description, amount, now()), "UPDATED");
    }

    @Transactional
    public Estimate approve(Long companyId, Long id) {
        Estimate previous = locked(companyId, id);
        if (previous.status() == EstimateStatus.APPROVED) return previous;
        return saveChange(previous, previous.approve(now()), "APPROVED");
    }

    @Transactional
    public void delete(Long companyId, Long id) {
        Estimate previous = locked(companyId, id);
        if (previous.status() != EstimateStatus.CANCELED) {
            saveChange(previous, previous.cancel(now()), "CANCELED");
        }
    }

    private Estimate saveChange(Estimate previous, Estimate next, String action) {
        Estimate saved = estimates.save(next);
        estimates.appendHistory(EstimateHistory.snapshot(saved, previous.status(), action));
        return saved;
    }

    private Estimate locked(Long companyId, Long id) {
        ensureCompany(companyId);
        return estimates.findForUpdate(id, companyId)
            .orElseThrow(() -> new NotFoundException("Estimate not found."));
    }

    private void ensureCompany(Long companyId) {
        companies.findById(companyId).orElseThrow(() -> new NotFoundException("Company not found."));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
}
