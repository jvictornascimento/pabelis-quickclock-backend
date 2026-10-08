package br.com.pabelis.quickclock.adapters.out.persistence.company;

import br.com.pabelis.quickclock.application.company.port.CompanyRepositoryPort;
import br.com.pabelis.quickclock.domain.company.Company;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class JpaCompanyRepositoryAdapter implements CompanyRepositoryPort {

    private final SpringDataCompanyRepository repository;

    JpaCompanyRepositoryAdapter(SpringDataCompanyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Company save(Company company) {
        return CompanyMapper.toDomain(repository.save(CompanyMapper.toEntity(company)));
    }

    @Override
    public List<Company> findAll() {
        return repository.findAll().stream()
            .map(CompanyMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Company> findById(Long id) {
        return repository.findById(id)
            .map(CompanyMapper::toDomain);
    }
}
