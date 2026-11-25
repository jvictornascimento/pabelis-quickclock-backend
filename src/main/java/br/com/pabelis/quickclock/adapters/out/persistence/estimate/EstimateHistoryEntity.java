package br.com.pabelis.quickclock.adapters.out.persistence.estimate;

import br.com.pabelis.quickclock.domain.estimate.EstimateHistory;
import br.com.pabelis.quickclock.domain.estimate.EstimateStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "estimate_history")
class EstimateHistoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false) Long estimateId;
    @Enumerated(EnumType.STRING) @Column(length = 30) EstimateStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) EstimateStatus newStatus;
    @Column(length = 30) String action;
    @Column(length = 500) String note;
    @Column(name = "snapshot_date") LocalDate date;
    @Column(name = "snapshot_description", length = 255) String description;
    @Column(name = "snapshot_amount_cents") Long amountCents;
    @Column(name = "snapshot_approved_at") LocalDateTime approvedAt;
    @Column(nullable = false) LocalDateTime createdAt;

    protected EstimateHistoryEntity() {}

    static EstimateHistoryEntity from(EstimateHistory history) {
        EstimateHistoryEntity entity = new EstimateHistoryEntity();
        entity.id = history.id();
        entity.estimateId = history.estimateId();
        entity.previousStatus = history.previousStatus();
        entity.newStatus = history.newStatus();
        entity.action = history.action();
        entity.note = history.note();
        entity.date = history.date();
        entity.description = history.description();
        entity.amountCents = history.amountCents();
        entity.approvedAt = history.approvedAt();
        entity.createdAt = history.createdAt();
        return entity;
    }

    EstimateHistory toDomain() {
        return new EstimateHistory(id, estimateId, previousStatus, newStatus, note, action, date, description,
            amountCents, approvedAt, createdAt);
    }
}
