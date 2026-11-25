package br.com.pabelis.quickclock.application.workday;

import br.com.pabelis.quickclock.application.company.CompanyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(WorkDayConcurrencyTest.FixedTime.class)
class WorkDayConcurrencyTest {
    @TestConfiguration
    static class FixedTime {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(Instant.parse("2026-11-25T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        }
    }

    @Autowired WorkDayService service;
    @Autowired CompanyService companies;

    @Test
    void concurrentFirstSavesReturnSameWorkDay() throws Exception {
        long id = companies.create("Concurrent saves").id();
        var date = LocalDate.of(2026, 11, 25);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return service.save(id, date, true, false); });
            var second = executor.submit(() -> { start.await(); return service.save(id, date, true, false); });
            start.countDown();
            assertEquals(first.get(15, TimeUnit.SECONDS).id(), second.get(15, TimeUnit.SECONDS).id());
            assertEquals(1, service.searchByMonth(id, YearMonth.from(date)).size());
        }
    }

    @Test
    void concurrentPeriodTogglesDoNotLoseUpdates() throws Exception {
        long id = companies.create("Concurrent toggles").id();
        var date = LocalDate.of(2026, 11, 25);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return service.toggleBeforeLunch(id, date); });
            var second = executor.submit(() -> { start.await(); return service.toggleAfterLunch(id, date); });
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
            var saved = service.get(id, date);
            assertTrue(saved.workedBeforeLunch());
            assertTrue(saved.workedAfterLunch());
            assertEquals(0L, saved.beforeLunchValueCents());
            assertEquals(0L, saved.afterLunchValueCents());
        }
    }
}
