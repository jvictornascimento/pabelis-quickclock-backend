package br.com.pabelis.quickclock.adapters.out.persistence.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_settings")
class CompanySettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private long halfDayValueCents;

    @Column(nullable = false)
    private boolean mondayActive;

    @Column(nullable = false)
    private boolean tuesdayActive;

    @Column(nullable = false)
    private boolean wednesdayActive;

    @Column(nullable = false)
    private boolean thursdayActive;

    @Column(nullable = false)
    private boolean fridayActive;

    @Column(nullable = false)
    private boolean saturdayActive;

    @Column(nullable = false)
    private boolean sundayActive;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected CompanySettingsEntity() {
    }

    CompanySettingsEntity(
        Long id,
        Long companyId,
        long halfDayValueCents,
        boolean mondayActive,
        boolean tuesdayActive,
        boolean wednesdayActive,
        boolean thursdayActive,
        boolean fridayActive,
        boolean saturdayActive,
        boolean sundayActive,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.companyId = companyId;
        this.halfDayValueCents = halfDayValueCents;
        this.mondayActive = mondayActive;
        this.tuesdayActive = tuesdayActive;
        this.wednesdayActive = wednesdayActive;
        this.thursdayActive = thursdayActive;
        this.fridayActive = fridayActive;
        this.saturdayActive = saturdayActive;
        this.sundayActive = sundayActive;
        this.updatedAt = updatedAt;
    }

    Long getId() {
        return id;
    }

    Long getCompanyId() {
        return companyId;
    }

    long getHalfDayValueCents() {
        return halfDayValueCents;
    }

    boolean isMondayActive() {
        return mondayActive;
    }

    boolean isTuesdayActive() {
        return tuesdayActive;
    }

    boolean isWednesdayActive() {
        return wednesdayActive;
    }

    boolean isThursdayActive() {
        return thursdayActive;
    }

    boolean isFridayActive() {
        return fridayActive;
    }

    boolean isSaturdayActive() {
        return saturdayActive;
    }

    boolean isSundayActive() {
        return sundayActive;
    }

    LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
