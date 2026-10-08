package br.com.pabelis.quickclock.application.company.port;

import br.com.pabelis.quickclock.domain.company.Company;

import java.util.List;
import java.util.Optional;

public interface CompanyRepositoryPort {

    Company save(Company company);

    List<Company> findAll();

    Optional<Company> findById(Long id);
}
