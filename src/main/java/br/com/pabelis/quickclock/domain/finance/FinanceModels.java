package br.com.pabelis.quickclock.domain.finance;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public final class FinanceModels {
    private FinanceModels() {}
    public enum SourceType { WORK_REPORT, ADDITIONAL_SERVICE, APPROVED_ESTIMATE }
    public enum PaymentMethod { CASH, PIX, DEBIT, CREDIT_CARD, OTHER }
    public enum EditScope { ONE, UNPAID }
    public record Card(Long id, String name, int closingDay, int dueDay) {
        public Card {
            name = FinanceRules.text(name, 120);
            FinanceRules.day(closingDay);
            FinanceRules.day(dueDay);
        }
    }
    public record Source(Long companyId, String companyName, SourceType sourceType, Long sourceId, LocalDate date,
                         String description, long amountCents) {}
    public record Income(Long id, Long companyId, String companyName, SourceType sourceType, Long sourceId,
                         LocalDate incomeDate, String description, long amountCents) {}
    public record Expense(Long id, LocalDate date, String description, long amountCents, String category,
                          PaymentMethod paymentMethod, Long cardId, String installmentGroupId,
                          int installmentNumber, int installmentTotal, boolean paid, LocalDate paidAt) {
        public Expense edit(String description, long amountCents, String category) {
            return new Expense(id, date, description, amountCents, category, paymentMethod, cardId,
                installmentGroupId, installmentNumber, installmentTotal, paid, paidAt);
        }
        public Expense pay(LocalDate paidAt) {
            return new Expense(id, date, description, amountCents, category, paymentMethod, cardId,
                installmentGroupId, installmentNumber, installmentTotal, true, paidAt);
        }
    }
    public record Summary(YearMonth month, long incomeCents, long expenseCents,
                          long expectedIncomeCents, long expectedExpenseCents, long balanceCents,
                          long projectedBalanceCents, List<Income> incomes, List<Expense> expenses,
                          List<Source> pendingIncome, List<Expense> paidExpenses) {}
}
