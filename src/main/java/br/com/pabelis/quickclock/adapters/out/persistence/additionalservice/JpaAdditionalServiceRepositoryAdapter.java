package br.com.pabelis.quickclock.adapters.out.persistence.additionalservice;

import br.com.pabelis.quickclock.application.additionalservice.port.AdditionalServiceRepositoryPort;
import br.com.pabelis.quickclock.application.finance.port.FinanceSourceGuard;
import br.com.pabelis.quickclock.domain.additionalservice.AdditionalService;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
class JpaAdditionalServiceRepositoryAdapter implements AdditionalServiceRepositoryPort {

    private final SpringDataAdditionalServiceRepository repository;
    private final FinanceSourceGuard financeGuard;

    JpaAdditionalServiceRepositoryAdapter(SpringDataAdditionalServiceRepository repository, FinanceSourceGuard financeGuard) {
        this.repository = repository;
        this.financeGuard = financeGuard;
    }

    @Override
    public AdditionalService save(AdditionalService service) {
        if (service.id() != null) financeGuard.lockAndAssertUnreceived("ADDITIONAL_SERVICE", service.id(), service.companyId());
        return AdditionalServiceMapper.toDomain(repository.save(AdditionalServiceMapper.toEntity(service)));
    }

    @Override
    public Optional<AdditionalService> findByIdAndCompanyId(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId)
            .map(AdditionalServiceMapper::toDomain);
    }

    @Override
    public List<AdditionalService> findByCompanyIdAndDateBetween(
        Long companyId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return repository.findByCompanyIdAndDateBetweenOrderByDateAsc(companyId, startDate, endDate).stream()
            .map(AdditionalServiceMapper::toDomain)
            .toList();
    }

    @Override
    public void delete(AdditionalService service) {
        financeGuard.lockAndAssertUnreceived("ADDITIONAL_SERVICE", service.id(), service.companyId());
        repository.delete(AdditionalServiceMapper.toEntity(service));
    }
}
