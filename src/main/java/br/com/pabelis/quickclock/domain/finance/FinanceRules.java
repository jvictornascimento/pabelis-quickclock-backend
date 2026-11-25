package br.com.pabelis.quickclock.domain.finance;

import br.com.pabelis.quickclock.domain.shared.BusinessException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class FinanceRules {
    private FinanceRules() {}

    public static List<Long> split(long total, int count) {
        money(total);
        if (count < 1 || count > 1200) throw new BusinessException("Installment total must be between 1 and 1200.");
        List<Long> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) result.add(total / count + (i < total % count ? 1 : 0));
        return List.copyOf(result);
    }

    public static List<LocalDate> dueDates(LocalDate purchase, int closingDay, int dueDay, int count) {
        day(closingDay);
        day(dueDay);
        YearMonth closingMonth = YearMonth.from(purchase);
        if (purchase.isAfter(atDay(closingMonth, closingDay))) closingMonth = closingMonth.plusMonths(1);
        YearMonth dueMonth = closingMonth;
        if (!atDay(dueMonth, dueDay).isAfter(atDay(closingMonth, closingDay))) dueMonth = dueMonth.plusMonths(1);
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < count; i++) dates.add(atDay(dueMonth.plusMonths(i), dueDay));
        return List.copyOf(dates);
    }

    private static LocalDate atDay(YearMonth month, int day) { return month.atDay(Math.min(day, month.lengthOfMonth())); }
    public static void day(int day) {
        if (day < 1 || day > 31) throw new BusinessException("Card day must be between 1 and 31.");
    }
    public static void money(long amount) {
        if (amount < 0) throw new BusinessException("Amount must be zero or greater.");
    }
    public static String text(String value, int max) {
        if (value == null || value.isBlank() || value.trim().length() > max)
            throw new BusinessException("Required text is empty or too long.");
        return value.trim();
    }
}
