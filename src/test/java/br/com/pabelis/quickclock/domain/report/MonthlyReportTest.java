package br.com.pabelis.quickclock.domain.report;

import br.com.pabelis.quickclock.domain.workday.WorkDay;
import br.com.pabelis.quickclock.domain.additionalservice.AdditionalService;
import br.com.pabelis.quickclock.domain.shared.BusinessException;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonthlyReportTest {
    private final YearMonth october = YearMonth.of(2026, 10);
    private final LocalDateTime now = october.atDay(1).atStartOfDay();

    @Test
    void matchesOctoberAcceptanceExampleWith41HistoricalPeriods() {
        List<WorkDay> days = new ArrayList<>();
        for (int day = 1; day <= october.lengthOfMonth(); day++) {
            LocalDate date = october.atDay(day);
            if (date.getDayOfWeek().getValue() <= 5 && day != 12) {
                days.add(WorkDay.create(1L, date, true, day != 20, 10000, now));
            }
        }
        MonthlyReport report = MonthlyReport.calculate(1L, "Example", october, days, List.of(), List.of());
        assertThat(report.workedDays()).isEqualTo(21);
        assertThat(report.workedPeriods()).isEqualTo(41);
        assertThat(report.totalValueCents()).isEqualTo(410000);
    }

    @Test
    void treatsLegacyNullSnapshotsAsZero() {
        WorkDay legacy = mock(WorkDay.class);
        when(legacy.workedBeforeLunch()).thenReturn(true);
        MonthlyReport report = MonthlyReport.calculate(1L, "Legacy", october, List.of(legacy), List.of(), List.of());
        assertThat(report.workedPeriods()).isEqualTo(1);
        assertThat(report.totalValueCents()).isZero();
    }

    @Test
    void rejectsOverflowInsideCategorySum() {
        List<AdditionalService> services = List.of(
            AdditionalService.create(1L, now.toLocalDate(), "One", Long.MAX_VALUE, now),
            AdditionalService.create(1L, now.toLocalDate(), "Two", 1, now));
        assertThatThrownBy(() -> MonthlyReport.calculate(1L, "Overflow", october, List.of(), services, List.of()))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void separatesGrossReceivedAndPendingWithoutChangingSourceValues() {
        MonthlyReport gross = MonthlyReport.calculate(1L, "Receipts", october, List.of(),
            List.of(AdditionalService.create(1L, now.toLocalDate(), "Service", 1000, now)), List.of());
        assertThat(gross.receivedValueCents()).isZero();
        assertThat(gross.pendingValueCents()).isEqualTo(1000);
        MonthlyReport partial = gross.withReceivedValueCents(400);
        assertThat(partial.totalValueCents()).isEqualTo(1000);
        assertThat(partial.receivedValueCents()).isEqualTo(400);
        assertThat(partial.pendingValueCents()).isEqualTo(600);
        assertThat(partial.additionalServicesValueCents()).isEqualTo(1000);
        MonthlyReport reversed = partial.withReceivedValueCents(0);
        assertThat(reversed.totalValueCents()).isEqualTo(1000);
        assertThat(reversed.pendingValueCents()).isEqualTo(1000);
    }

    @Test
    void rejectsNegativeOrExcessReceiptsInsteadOfReturningInvalidPendingValue() {
        MonthlyReport empty = MonthlyReport.calculate(1L, "Empty", october, List.of(), List.of(), List.of());
        assertThatThrownBy(() -> empty.withReceivedValueCents(-1)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> empty.withReceivedValueCents(1)).isInstanceOf(BusinessException.class);
    }
}
