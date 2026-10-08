package br.com.pabelis.quickclock.adapters.out.persistence.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SpringDataCompanySettingsRepository extends JpaRepository<CompanySettingsEntity, Long> {

    Optional<CompanySettingsEntity> findByCompanyId(Long companyId);
}
