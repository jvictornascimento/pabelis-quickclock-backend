package br.com.pabelis.quickclock.adapters.out.persistence.company;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCompanyRepository extends JpaRepository<CompanyEntity, Long> {
}
