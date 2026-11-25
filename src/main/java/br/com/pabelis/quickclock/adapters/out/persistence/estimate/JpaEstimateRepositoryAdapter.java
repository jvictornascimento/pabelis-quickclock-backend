package br.com.pabelis.quickclock.adapters.out.persistence.estimate;

import br.com.pabelis.quickclock.application.estimate.port.EstimateRepositoryPort;
import br.com.pabelis.quickclock.domain.estimate.Estimate;
import br.com.pabelis.quickclock.domain.estimate.EstimateHistory;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
class JpaEstimateRepositoryAdapter implements EstimateRepositoryPort {
    private final SpringDataEstimateRepository repository;
    private final SpringDataEstimateHistoryRepository history;

    JpaEstimateRepositoryAdapter(SpringDataEstimateRepository repository, SpringDataEstimateHistoryRepository history) {
        this.repository = repository;
        this.history = history;
    }
    @Override public Estimate save(Estimate estimate) {
        return repository.save(EstimateEntity.from(estimate)).toDomain();
    }
    @Override public Optional<Estimate> findByIdAndCompanyId(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(EstimateEntity::toDomain);
    }
    @Override public Optional<Estimate> findForUpdate(Long id, Long companyId) {
        return repository.findForUpdate(id, companyId).map(EstimateEntity::toDomain);
    }
    @Override public List<Estimate> findByCompanyId(Long companyId) {
        return repository.findByCompanyIdOrderByDateAscIdAsc(companyId).stream().map(EstimateEntity::toDomain).toList();
    }
    @Override public List<Estimate> findByCompanyIdAndDateBetween(Long companyId, LocalDate start, LocalDate end) {
        return repository.findByCompanyIdAndDateBetweenOrderByDateAscIdAsc(companyId, start, end).stream()
            .map(EstimateEntity::toDomain).toList();
    }
    @Override public List<Estimate> findApprovedByCompanyIdAndApprovedAtRange(Long companyId, LocalDateTime start, LocalDateTime end) {
        return repository.findApproved(companyId, start, end).stream().map(EstimateEntity::toDomain).toList();
    }
    @Override public void appendHistory(EstimateHistory snapshot) {
        history.save(EstimateHistoryEntity.from(snapshot));
    }
    @Override public List<EstimateHistory> findHistory(Long estimateId) {
        return history.findByEstimateIdOrderByIdAsc(estimateId).stream().map(EstimateHistoryEntity::toDomain).toList();
    }
}
