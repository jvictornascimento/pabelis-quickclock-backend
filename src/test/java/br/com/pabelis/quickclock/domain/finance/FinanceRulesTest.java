package br.com.pabelis.quickclock.domain.finance;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

class FinanceRulesTest {
    @Test void splitsEveryCentDeterministically() {
        assertThat(FinanceRules.split(100, 3)).containsExactly(34L, 33L, 33L);
        assertThat(FinanceRules.split(2, 3)).containsExactly(1L, 1L, 0L);
    }
    @Test void clampsClosingAndDueDaysWithoutDriftingLaterInstallments() {
        assertThat(FinanceRules.dueDates(LocalDate.of(2028, 1, 31), 31, 30, 3))
            .containsExactly(LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 30), LocalDate.of(2028, 4, 30));
    }
    @Test void purchaseAfterClosingMovesToNextInvoiceAcrossYear() {
        assertThat(FinanceRules.dueDates(LocalDate.of(2026, 12, 21), 20, 5, 2))
            .containsExactly(LocalDate.of(2027, 2, 5), LocalDate.of(2027, 3, 5));
    }
    @Test void closingDayIsInclusiveAndDueCanBeInSameMonth() {
        assertThat(FinanceRules.dueDates(LocalDate.of(2026, 11, 10), 10, 20, 1))
            .containsExactly(LocalDate.of(2026, 11, 20));
    }
    @Test void rejectsInvalidMoneyAndInstallments() {
        assertThatThrownBy(() -> FinanceRules.split(-1, 2)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> FinanceRules.split(100, 0)).isInstanceOf(RuntimeException.class);
    }
}
