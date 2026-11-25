package br.com.pabelis.quickclock.adapters.out.persistence.estimate;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface SpringDataEstimateHistoryRepository extends JpaRepository<EstimateHistoryEntity, Long> {
    List<EstimateHistoryEntity> findByEstimateIdOrderByIdAsc(Long estimateId);
}
