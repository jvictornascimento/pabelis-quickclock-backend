package br.com.pabelis.quickclock.domain.estimate;

import br.com.pabelis.quickclock.domain.shared.BusinessException;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

class EstimateTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 12, 1, 0, 0);

    @Test
    void retainsOriginalAndApprovalTimestampAndRejectsApprovedMutations() {
        Estimate original = Estimate.create(1L, LocalDate.of(2026, 1, 1), " Original ", 100, now);
        Estimate updated = original.update(LocalDate.of(2026, 2, 1), "Edited", 200, now.plusDays(1));
        Estimate approved = updated.approve(now.plusDays(2));
        assertThat(original.description()).isEqualTo("Original");
        assertThat(original.amountCents()).isEqualTo(100);
        assertThat(approved.approve(now.plusMonths(1))).isSameAs(approved);
        assertThat(approved.approvedAt()).isEqualTo(now.plusDays(2));
        assertThat(approved.createdAt()).isEqualTo(now);
        assertThatThrownBy(() -> approved.update(original.date(), "Changed", 0, now))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> approved.cancel(now)).isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsInvalidDraftsAndCanceledApproval() {
        assertThatThrownBy(() -> Estimate.create(1L, null, "Description", 1, now)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> Estimate.create(1L, now.toLocalDate(), " ", 1, now)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> Estimate.create(1L, now.toLocalDate(), "x".repeat(256), 1, now)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> Estimate.create(1L, now.toLocalDate(), "Description", -1, now)).isInstanceOf(BusinessException.class);
        Estimate canceled = Estimate.create(1L, now.toLocalDate(), "Description", 0, now).cancel(now);
        assertThat(canceled.cancel(now.plusDays(1))).isSameAs(canceled);
        assertThatThrownBy(() -> canceled.approve(now)).isInstanceOf(BusinessException.class);
    }
}
