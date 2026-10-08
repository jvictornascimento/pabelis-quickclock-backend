package br.com.pabelis.quickclock.application.company.port;

import br.com.pabelis.quickclock.domain.company.CompanySettings;

import java.util.Optional;

public interface CompanySettingsRepositoryPort {

    CompanySettings save(CompanySettings settings);

    Optional<CompanySettings> findByCompanyId(Long companyId);
}
