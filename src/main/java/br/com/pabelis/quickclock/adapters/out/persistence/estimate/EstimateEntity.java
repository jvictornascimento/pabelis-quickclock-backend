package br.com.pabelis.quickclock.adapters.out.persistence.estimate;

import br.com.pabelis.quickclock.domain.estimate.Estimate;
import br.com.pabelis.quickclock.domain.estimate.EstimateStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "estimate")
class EstimateEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false) Long companyId;
    @Column(name = "estimate_date", nullable = false) LocalDate date;
    @Column(nullable = false, length = 255) String description;
    @Column(nullable = false) long amountCents;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) EstimateStatus status;
    LocalDateTime approvedAt;
    @Column(nullable = false) LocalDateTime createdAt;
    @Column(nullable = false) LocalDateTime updatedAt;

    protected EstimateEntity() {}

    static EstimateEntity from(Estimate estimate) {
        EstimateEntity entity = new EstimateEntity();
        entity.id = estimate.id();
        entity.companyId = estimate.companyId();
        entity.date = estimate.date();
        entity.description = estimate.description();
        entity.amountCents = estimate.amountCents();
        entity.status = estimate.status();
        entity.approvedAt = estimate.approvedAt();
        entity.createdAt = estimate.createdAt();
        entity.updatedAt = estimate.updatedAt();
        return entity;
    }

    Estimate toDomain() {
        return new Estimate(id, companyId, date, description, amountCents, status, approvedAt, createdAt, updatedAt);
    }
}
