package br.com.pabelis.quickclock.adapters.out.persistence.additionalservice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "additional_service")
class AdditionalServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(name = "service_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false)
    private long amountCents;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected AdditionalServiceEntity() {
    }

    AdditionalServiceEntity(
        Long id,
        Long companyId,
        LocalDate date,
        String description,
        long amountCents,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.companyId = companyId;
        this.date = date;
        this.description = description;
        this.amountCents = amountCents;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    Long getId() {
        return id;
    }

    Long getCompanyId() {
        return companyId;
    }

    LocalDate getDate() {
        return date;
    }

    String getDescription() {
        return description;
    }

    long getAmountCents() {
        return amountCents;
    }

    LocalDateTime getCreatedAt() {
        return createdAt;
    }

    LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
