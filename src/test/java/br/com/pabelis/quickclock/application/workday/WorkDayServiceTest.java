package br.com.pabelis.quickclock.application.workday;

import br.com.pabelis.quickclock.application.company.port.CompanyRepositoryPort;
import br.com.pabelis.quickclock.application.company.port.CompanySettingsRepositoryPort;
import br.com.pabelis.quickclock.application.finance.port.FinanceSourceGuard;
import br.com.pabelis.quickclock.application.workday.port.WorkDayRepositoryPort;
import br.com.pabelis.quickclock.domain.company.Company;
import br.com.pabelis.quickclock.domain.company.CompanySettings;
import br.com.pabelis.quickclock.domain.company.WeekdaySettings;
import br.com.pabelis.quickclock.domain.workday.WorkDay;
import br.com.pabelis.quickclock.application.TimeConfig;
import br.com.pabelis.quickclock.domain.shared.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class WorkDayServiceTest {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Test
    void rejectsNullDateAsBusinessValidation() {
        assertThrows(BusinessException.class, () -> new br.com.pabelis.quickclock.domain.workday.WorkDayEditPolicy()
            .ensureCanEdit(null, LocalDate.of(2026, 11, 25)));
    }

    @Test
    void productionClockUsesSaoPaulo() {
        var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext(TimeConfig.class);
        try (context) {
            assertEquals(ZONE, context.getBean(Clock.class).getZone());
        }
    }

    @Test
    void changesEditableDateExactlyAtSaoPauloMidnight() {
        var days = mock(WorkDayRepositoryPort.class);
        var companies = mock(CompanyRepositoryPort.class);
        var settings = mock(CompanySettingsRepositoryPort.class);
        var now = LocalDateTime.of(2026, 11, 25, 0, 0);
        when(companies.findById(1L)).thenReturn(Optional.of(new Company(1L, "Active", true, now, now)));
        when(settings.findByCompanyId(1L)).thenReturn(Optional.of(CompanySettings.defaultFor(1L, now)));
        when(days.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var before = new WorkDayService(days, companies, settings, mock(FinanceSourceGuard.class),
            Clock.fixed(Instant.parse("2026-11-26T02:59:59Z"), ZONE));
        assertEquals(LocalDate.of(2026, 11, 25), before.save(1L, LocalDate.of(2026, 11, 25), true, false).date());
        assertThrows(BusinessException.class, () -> before.save(1L, LocalDate.of(2026, 11, 26), true, false));
        var after = new WorkDayService(days, companies, settings, mock(FinanceSourceGuard.class),
            Clock.fixed(Instant.parse("2026-11-26T03:00:00Z"), ZONE));
        assertEquals(LocalDate.of(2026, 11, 26), after.save(1L, LocalDate.of(2026, 11, 26), true, false).date());
        assertThrows(BusinessException.class, () -> after.save(1L, LocalDate.of(2026, 11, 25), true, false));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void respectsEveryConfiguredWeekday(int offset) {
        var days = mock(WorkDayRepositoryPort.class);
        var companies = mock(CompanyRepositoryPort.class);
        var settings = mock(CompanySettingsRepositoryPort.class);
        var date = LocalDate.of(2026, 11, 23).plusDays(offset);
        var now = date.atTime(12, 0);
        when(companies.findById(1L)).thenReturn(Optional.of(new Company(1L, "Active", true, now, now)));
        when(days.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new WorkDayService(days, companies, settings, mock(FinanceSourceGuard.class),
            Clock.fixed(now.atZone(ZONE).toInstant(), ZONE));
        var enabled = new WeekdaySettings(offset == 0, offset == 1, offset == 2, offset == 3,
            offset == 4, offset == 5, offset == 6);
        when(settings.findByCompanyId(1L)).thenReturn(Optional.of(new CompanySettings(1L, 1L, 123L, enabled, now)));
        assertEquals(123L, service.save(1L, date, true, false).beforeLunchValueCents());
        when(settings.findByCompanyId(1L)).thenReturn(Optional.of(new CompanySettings(1L, 1L, 123L,
            new WeekdaySettings(false, false, false, false, false, false, false), now)));
        assertThrows(BusinessException.class, () -> service.save(1L, date, true, false));
        var order = inOrder(days, companies);
        order.verify(days).lockCompany(1L);
        order.verify(companies).findById(1L);
    }

    @Test
    void rejectsNegativeOrMissingMarkedValues() {
        var today = LocalDate.of(2026, 11, 25);
        var now = today.atStartOfDay();
        assertThrows(BusinessException.class, () -> new WorkDay(null, 1L, today, true, false, -1L, null, now, now));
        assertThrows(BusinessException.class, () -> new WorkDay(null, 1L, today, false, true, null, -1L, now, now));
        assertThrows(BusinessException.class, () -> new WorkDay(null, 1L, today, true, false, null, null, now, now));
    }

    @Test
    void rejectsInactiveCompanyBeforeSavingOrToggling() {
        var days = mock(WorkDayRepositoryPort.class);
        var companies = mock(CompanyRepositoryPort.class);
        var now = LocalDateTime.of(2026, 11, 25, 12, 0);
        when(companies.findById(1L)).thenReturn(Optional.of(new Company(1L, "Inactive", false, now, now)));
        var clock = Clock.fixed(Instant.parse("2026-11-25T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        var service = new WorkDayService(days, companies, mock(CompanySettingsRepositoryPort.class),
            mock(FinanceSourceGuard.class), clock);
        var today = LocalDate.of(2026, 11, 25);
        assertThrows(BusinessException.class, () -> service.save(1L, today, true, false));
        assertThrows(BusinessException.class, () -> service.toggleBeforeLunch(1L, today));
        assertThrows(BusinessException.class, () -> service.toggleAfterLunch(1L, today));
        verify(days, never()).save(any());
    }
}
