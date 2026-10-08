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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.companyId = companyId;
        this.date = date;
        this.workedBeforeLunch = workedBeforeLunch;
        this.workedAfterLunch = workedAfterLunch;
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

    LocalDateTime getCreatedAt() {
        return createdAt;
    }

    LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
