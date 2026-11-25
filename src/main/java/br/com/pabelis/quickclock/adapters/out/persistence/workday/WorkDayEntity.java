package br.com.pabelis.quickclock.adapters.out.persistence.workday;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_day")
class WorkDayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(name = "work_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private boolean workedBeforeLunch;

    @Column(nullable = false)
    private boolean workedAfterLunch;

    private Long beforeLunchValueCents;

    private Long afterLunchValueCents;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected WorkDayEntity() {
    }

    WorkDayEntity(
        Long id,
        Long companyId,
        LocalDate date,
        boolean workedBeforeLunch,
        boolean workedAfterLunch,
        Long beforeLunchValueCents,
        Long afterLunchValueCents,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.companyId = companyId;
        this.date = date;
        this.workedBeforeLunch = workedBeforeLunch;
        this.workedAfterLunch = workedAfterLunch;
        this.beforeLunchValueCents = beforeLunchValueCents;
        this.afterLunchValueCents = afterLunchValueCents;
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

    boolean isWorkedBeforeLunch() {
        return workedBeforeLunch;
    }

    boolean isWorkedAfterLunch() {
        return workedAfterLunch;
    }

    Long getBeforeLunchValueCents() {
        return beforeLunchValueCents;
    }

    Long getAfterLunchValueCents() {
        return afterLunchValueCents;
    }

    LocalDateTime getCreatedAt() {
        return createdAt;
    }

    LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
