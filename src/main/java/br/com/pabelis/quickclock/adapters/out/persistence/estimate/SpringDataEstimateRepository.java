package br.com.pabelis.quickclock.adapters.out.persistence.estimate;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

interface SpringDataEstimateRepository extends JpaRepository<EstimateEntity, Long> {
    Optional<EstimateEntity> findByIdAndCompanyId(Long id, Long companyId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EstimateEntity e where e.id = :id and e.companyId = :companyId")
    Optional<EstimateEntity> findForUpdate(@Param("id") Long id, @Param("companyId") Long companyId);
    List<EstimateEntity> findByCompanyIdOrderByDateAscIdAsc(Long companyId);
    List<EstimateEntity> findByCompanyIdAndDateBetweenOrderByDateAscIdAsc(Long companyId, LocalDate start, LocalDate end);
    @Query("select e from EstimateEntity e where e.companyId = :companyId and e.status = br.com.pabelis.quickclock.domain.estimate.EstimateStatus.APPROVED and e.approvedAt >= :start and e.approvedAt < :end order by e.approvedAt, e.id")
    List<EstimateEntity> findApproved(@Param("companyId") Long companyId, @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end);
}
