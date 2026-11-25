package br.com.pabelis.quickclock.application.finance;

import br.com.pabelis.quickclock.domain.finance.FinanceModels.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FinanceConcurrencyTest {
    @Autowired FinanceService finance;
    @Autowired JdbcTemplate jdbc;

    @Test void concurrentReceiptsReturnOnePersistedIncome() throws Exception {
        String name = "Finance concurrency " + java.util.UUID.randomUUID();
        jdbc.update("INSERT INTO company(name,active,created_at,updated_at) VALUES (?,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",name);
        Long company = jdbc.queryForObject("SELECT id FROM company WHERE name=?",Long.class,name);
        jdbc.update("INSERT INTO additional_service(company_id,service_date,description,amount_cents,created_at,updated_at) VALUES (?,DATE '2026-01-01','Concurrent',123,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",company);
        Long source = jdbc.queryForObject("SELECT id FROM additional_service WHERE company_id=?",Long.class,company);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
            java.util.concurrent.Callable<Income> receive = () -> {
                ready.countDown();
                if (!start.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
                return finance.receive(company,SourceType.ADDITIONAL_SERVICE,source,LocalDate.of(2026,1,2));
            };
            var first = executor.submit(receive);
            var second = executor.submit(receive);
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(first.get(30,TimeUnit.SECONDS).id()).isEqualTo(second.get(30,TimeUnit.SECONDS).id());
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM financial_income WHERE company_id=?",Long.class,company)).isEqualTo(1);
        } finally {
            jdbc.update("DELETE FROM financial_income WHERE company_id=?",company);
            jdbc.update("DELETE FROM additional_service WHERE company_id=?",company);
            jdbc.update("DELETE FROM company WHERE id=?",company);
        }
    }
}
