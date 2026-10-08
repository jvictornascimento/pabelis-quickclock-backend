package br.com.pabelis.quickclock.adapters.out.persistence.company;

import br.com.pabelis.quickclock.application.company.port.CompanySettingsRepositoryPort;
import br.com.pabelis.quickclock.domain.company.CompanySettings;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaCompanySettingsRepositoryAdapter implements CompanySettingsRepositoryPort {

    private final SpringDataCompanySettingsRepository repository;

    JpaCompanySettingsRepositoryAdapter(SpringDataCompanySettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompanySettings save(CompanySettings settings) {
        return CompanyMapper.toDomain(repository.save(CompanyMapper.toEntity(settings)));
    }

    @Override
    public Optional<CompanySettings> findByCompanyId(Long companyId) {
        return repository.findByCompanyId(companyId)
            .map(CompanyMapper::toDomain);
    }
}
