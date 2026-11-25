package br.com.pabelis.quickclock.application.estimate.port;

import br.com.pabelis.quickclock.domain.estimate.Estimate;
import br.com.pabelis.quickclock.domain.estimate.EstimateHistory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EstimateRepositoryPort {
    Estimate save(Estimate estimate);
    Optional<Estimate> findByIdAndCompanyId(Long id, Long companyId);
    // Implementations must serialize mutations of an existing estimate within the transaction.
    Optional<Estimate> findForUpdate(Long id, Long companyId);
    List<Estimate> findByCompanyId(Long companyId);
    List<Estimate> findByCompanyIdAndDateBetween(Long companyId, LocalDate start, LocalDate end);
    List<Estimate> findApprovedByCompanyIdAndApprovedAtRange(Long companyId, LocalDateTime start, LocalDateTime endExclusive);
    void appendHistory(EstimateHistory history);
    List<EstimateHistory> findHistory(Long estimateId);
}
