package br.com.pabelis.quickclock.adapters.out.persistence.workday;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface SpringDataWorkDayRepository extends JpaRepository<WorkDayEntity, Long> {

    Optional<WorkDayEntity> findByCompanyIdAndDate(Long companyId, LocalDate date);

    List<WorkDayEntity> findByCompanyIdAndDateBetweenOrderByDateAsc(Long companyId, LocalDate startDate, LocalDate endDate);
}
