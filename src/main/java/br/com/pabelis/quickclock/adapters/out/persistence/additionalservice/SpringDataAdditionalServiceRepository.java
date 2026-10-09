package br.com.pabelis.quickclock.adapters.out.persistence.additionalservice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface SpringDataAdditionalServiceRepository extends JpaRepository<AdditionalServiceEntity, Long> {

    Optional<AdditionalServiceEntity> findByIdAndCompanyId(Long id, Long companyId);

    List<AdditionalServiceEntity> findByCompanyIdAndDateBetweenOrderByDateAsc(
        Long companyId,
        LocalDate startDate,
        LocalDate endDate
    );
}
