package br.com.pabelis.quickclock.application.report;

import br.com.pabelis.quickclock.application.company.port.CompanyRepositoryPort;
import br.com.pabelis.quickclock.application.workday.port.WorkDayRepositoryPort;
import br.com.pabelis.quickclock.application.additionalservice.port.AdditionalServiceRepositoryPort;
import br.com.pabelis.quickclock.application.estimate.port.EstimateRepositoryPort;
import br.com.pabelis.quickclock.application.finance.port.FinanceRepositoryPort;
import br.com.pabelis.quickclock.domain.finance.FinanceModels.Income;
import br.com.pabelis.quickclock.domain.finance.FinanceModels.SourceType;
import br.com.pabelis.quickclock.domain.company.Company;
import br.com.pabelis.quickclock.domain.report.MonthlyReport;
import br.com.pabelis.quickclock.domain.shared.NotFoundException;
import br.com.pabelis.quickclock.domain.shared.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.YearMonth;

@Service
public class MonthlyReportService {
    private final CompanyRepositoryPort companies;
    private final WorkDayRepositoryPort workDays;
    private final AdditionalServiceRepositoryPort services;
    private final EstimateRepositoryPort estimates;
    private final FinanceRepositoryPort finance;

    public MonthlyReportService(CompanyRepositoryPort companies, WorkDayRepositoryPort workDays,
        AdditionalServiceRepositoryPort services, EstimateRepositoryPort estimates, FinanceRepositoryPort finance) {
        this.companies = companies;
        this.workDays = workDays;
        this.services = services;
        this.estimates = estimates;
        this.finance = finance;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public MonthlyReport get(Long companyId, YearMonth month) {
        Company company = companies.findById(companyId)
            .orElseThrow(() -> new NotFoundException("Company not found."));
        MonthlyReport report = MonthlyReport.calculate(companyId, company.name(), month,
            workDays.findByCompanyIdAndDateBetween(companyId, month.atDay(1), month.atEndOfMonth()),
            services.findByCompanyIdAndDateBetween(companyId, month.atDay(1), month.atEndOfMonth()),
            estimates.findApprovedByCompanyIdAndApprovedAtRange(companyId,
                month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay()));
        return report.withReceivedValueCents(receivedValueCents(report));
    }

    private long receivedValueCents(MonthlyReport report) {
        try {
            long received = 0;
            for (var day : report.workDays()) {
                received = Math.addExact(received, receiptValue(report.companyId(), SourceType.WORK_REPORT, day.id()));
            }
            for (var service : report.additionalServices()) {
                received = Math.addExact(received, receiptValue(report.companyId(), SourceType.ADDITIONAL_SERVICE, service.id()));
            }
            for (var estimate : report.approvedEstimates()) {
                received = Math.addExact(received, receiptValue(report.companyId(), SourceType.APPROVED_ESTIMATE, estimate.id()));
            }
            return received;
        } catch (ArithmeticException exception) {
            throw new BusinessException("Report received value exceeds the supported monetary range.");
        }
    }

    private long receiptValue(Long companyId, SourceType type, Long sourceId) {
        // Lookup by origin, not payment month; reversed receipts are absent from this port.
        return finance.income(type, sourceId)
            .filter(income -> companyId.equals(income.companyId()))
            .map(Income::amountCents).orElse(0L);
    }
}
