package br.com.pabelis.quickclock.domain.workday;

import br.com.pabelis.quickclock.domain.shared.BusinessException;

import java.time.LocalDate;

public class WorkDayEditPolicy {

    public void ensureCanEdit(LocalDate targetDate, LocalDate today) {
        if (targetDate.isBefore(today)) {
            throw new BusinessException("Old work days are read only.");
        }
        if (targetDate.isAfter(today)) {
            throw new BusinessException("Future work days cannot be edited.");
        }
    }
}
