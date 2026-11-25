package br.com.pabelis.quickclock.application.estimate;

import br.com.pabelis.quickclock.application.company.CompanyService;
import br.com.pabelis.quickclock.domain.estimate.Estimate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDate;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EstimateConcurrencyTest {
    @Autowired EstimateService estimates;
    @Autowired CompanyService companies;

    @Test
    void concurrentApprovalsCommitOnlyOneHistoryAndRetainSameTimestamp() throws Exception {
        long company = companies.create("Concurrent estimates").id();
        Estimate draft = estimates.create(company, LocalDate.of(2026, 1, 1), "Proposal", 1000);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Estimate> approve = () -> {
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                return estimates.approve(company, draft.id());
            };
            Future<Estimate> first = pool.submit(approve);
            Future<Estimate> second = pool.submit(approve);
            start.countDown();
            Estimate firstResult = first.get(10, TimeUnit.SECONDS);
            Estimate secondResult = second.get(10, TimeUnit.SECONDS);
            assertThat(firstResult.approvedAt()).isEqualTo(secondResult.approvedAt());
            assertThat(estimates.get(company, draft.id()).approvedAt()).isEqualTo(firstResult.approvedAt());
            assertThat(estimates.history(company, draft.id())).hasSize(2);
            assertThat(estimates.history(company, draft.id()).get(1).action()).isEqualTo("APPROVED");
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
