package br.com.pabelis.quickclock.adapters.in.web.report;

import br.com.pabelis.quickclock.domain.report.MonthlyReport;
import br.com.pabelis.quickclock.adapters.in.web.estimate.EstimateResponse;
import br.com.pabelis.quickclock.domain.additionalservice.AdditionalService;
import br.com.pabelis.quickclock.domain.workday.WorkDay;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record MonthlyReportResponse(
    Long companyId, String companyName, YearMonth month, List<WorkDayLine> workDays,
    List<ServiceLine> additionalServices, List<EstimateResponse> approvedEstimates,
    int workedDays, int workedPeriods, long workDaysValueCents, long additionalServicesValueCents,
    long approvedEstimatesValueCents, long totalValueCents, long receivedValueCents, long pendingValueCents
) {
    public static MonthlyReportResponse from(MonthlyReport report) {
        return new MonthlyReportResponse(report.companyId(), report.companyName(), report.month(),
            report.workDays().stream().map(WorkDayLine::from).toList(),
            report.additionalServices().stream().map(ServiceLine::from).toList(),
            report.approvedEstimates().stream().map(EstimateResponse::from).toList(), report.workedDays(),
            report.workedPeriods(), report.workDaysValueCents(), report.additionalServicesValueCents(),
            report.approvedEstimatesValueCents(), report.totalValueCents(), report.receivedValueCents(), report.pendingValueCents());
    }

    public record WorkDayLine(Long id, Long companyId, LocalDate date, boolean workedBeforeLunch,
        boolean workedAfterLunch, Long beforeLunchValueCents, Long afterLunchValueCents, long amountCents) {
        static WorkDayLine from(WorkDay day) {
            long before = day.workedBeforeLunch() && day.beforeLunchValueCents() != null ? day.beforeLunchValueCents() : 0;
            long after = day.workedAfterLunch() && day.afterLunchValueCents() != null ? day.afterLunchValueCents() : 0;
            return new WorkDayLine(day.id(), day.companyId(), day.date(), day.workedBeforeLunch(),
                day.workedAfterLunch(), day.beforeLunchValueCents(), day.afterLunchValueCents(), Math.addExact(before, after));
        }
    }
    public record ServiceLine(Long id, Long companyId, LocalDate date, String description, long amountCents) {
        static ServiceLine from(AdditionalService service) {
            return new ServiceLine(service.id(), service.companyId(), service.date(), service.description(), service.amountCents());
        }
    }
}
