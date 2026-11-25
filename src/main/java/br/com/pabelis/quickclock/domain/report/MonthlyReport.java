package br.com.pabelis.quickclock.domain.report;

import br.com.pabelis.quickclock.domain.additionalservice.AdditionalService;
import br.com.pabelis.quickclock.domain.estimate.Estimate;
import br.com.pabelis.quickclock.domain.workday.WorkDay;
import br.com.pabelis.quickclock.domain.shared.BusinessException;
import java.time.YearMonth;
import java.util.List;

/**
 * Gross value belongs to the report's source month, not to the cash-flow month.
 * Received value includes active receipts for these origins on any payment date.
 * Reversing a receipt restores pending value; gross value never changes because of payment.
 */
public record MonthlyReport(
    Long companyId, String companyName, YearMonth month,
    List<WorkDay> workDays, List<AdditionalService> additionalServices, List<Estimate> approvedEstimates,
    int workedDays, int workedPeriods, long workDaysValueCents, long additionalServicesValueCents,
    long approvedEstimatesValueCents, long totalValueCents, long receivedValueCents, long pendingValueCents
) {
    public MonthlyReport {
        workDays = List.copyOf(workDays);
        additionalServices = List.copyOf(additionalServices);
        approvedEstimates = List.copyOf(approvedEstimates);
    }

    public static MonthlyReport calculate(Long companyId, String companyName, YearMonth month,
        List<WorkDay> days, List<AdditionalService> services, List<Estimate> estimates) {
        List<WorkDay> marked = days.stream()
            .filter(day -> day.workedBeforeLunch() || day.workedAfterLunch()).toList();
        try {
            int periods = 0;
            long dayValue = 0;
            long servicesValue = 0;
            long estimatesValue = 0;
            for (WorkDay day : marked) {
                if (day.workedBeforeLunch()) {
                    periods = Math.addExact(periods, 1);
                    dayValue = Math.addExact(dayValue, valueOrZero(day.beforeLunchValueCents()));
                }
                if (day.workedAfterLunch()) {
                    periods = Math.addExact(periods, 1);
                    dayValue = Math.addExact(dayValue, valueOrZero(day.afterLunchValueCents()));
                }
            }
            for (AdditionalService service : services) servicesValue = Math.addExact(servicesValue, service.amountCents());
            for (Estimate estimate : estimates) estimatesValue = Math.addExact(estimatesValue, estimate.amountCents());
            long total = Math.addExact(Math.addExact(dayValue, servicesValue), estimatesValue);
            return new MonthlyReport(companyId, companyName, month, marked, services, estimates,
                marked.size(), periods, dayValue, servicesValue, estimatesValue, total, 0, total);
        } catch (ArithmeticException exception) {
            throw new BusinessException("Report total exceeds the supported monetary range.");
        }
    }

    public MonthlyReport withReceivedValueCents(long received) {
        if (received < 0 || received > totalValueCents) {
            throw new BusinessException("Received value must be between zero and the report gross value.");
        }
        return new MonthlyReport(companyId, companyName, month, workDays, additionalServices, approvedEstimates,
            workedDays, workedPeriods, workDaysValueCents, additionalServicesValueCents,
            approvedEstimatesValueCents, totalValueCents, received, Math.subtractExact(totalValueCents, received));
    }

    private static long valueOrZero(Long value) { return value == null ? 0 : value; }
}
