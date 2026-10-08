package br.com.pabelis.quickclock.domain.company;

import br.com.pabelis.quickclock.domain.shared.BusinessException;

import java.time.LocalDateTime;

public record Company(
    Long id,
    String name,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public Company {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Company name is required.");
        }
        if (name.trim().length() > 120) {
            throw new BusinessException("Company name must have at most 120 characters.");
        }
        name = name.trim();
    }

    public static Company create(String name, LocalDateTime now) {
        return new Company(null, name, true, now, now);
    }

    public Company rename(String newName, LocalDateTime now) {
        return new Company(id, newName, active, createdAt, now);
    }

    public Company deactivate(LocalDateTime now) {
        return new Company(id, name, false, createdAt, now);
    }
}
