package br.com.pabelis.quickclock.application.finance.port;

import br.com.pabelis.quickclock.domain.finance.FinanceModels.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface FinanceRepositoryPort {
    List<Card> cards();
    Card saveCard(Card card);
    Optional<Card> card(Long id);
    Source lockSource(SourceType type, Long sourceId, Long companyId);
    Optional<Income> income(SourceType type, Long sourceId);
    Optional<Income> income(Long incomeId, Long companyId);
    Income insertIncome(Source source, LocalDate date);
    void deleteIncome(Long id);
    List<Income> incomes(YearMonth month);
    List<Source> pending(YearMonth month);
    Expense saveExpense(Expense expense);
    Optional<Expense> expense(Long id);
    List<Expense> lockGroup(String group);
    List<Expense> expenses(YearMonth month);
    List<Expense> paidExpenses(YearMonth month);
    void deleteExpense(Long id);
}
