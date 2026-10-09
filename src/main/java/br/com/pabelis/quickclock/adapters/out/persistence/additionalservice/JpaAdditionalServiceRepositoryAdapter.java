package br.com.pabelis.quickclock.adapters.out.persistence.additionalservice;

import br.com.pabelis.quickclock.application.additionalservice.port.AdditionalServiceRepositoryPort;
import br.com.pabelis.quickclock.domain.additionalservice.AdditionalService;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
class JpaAdditionalServiceRepositoryAdapter implements AdditionalServiceRepositoryPort {

    private final SpringDataAdditionalServiceRepository repository;

    JpaAdditionalServiceRepositoryAdapter(SpringDataAdditionalServiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public AdditionalService save(AdditionalService service) {
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
        repository.delete(AdditionalServiceMapper.toEntity(service));
    }
}
