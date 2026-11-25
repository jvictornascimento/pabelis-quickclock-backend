package br.com.pabelis.quickclock.application.finance;

import br.com.pabelis.quickclock.application.finance.port.FinanceRepositoryPort;
import br.com.pabelis.quickclock.domain.finance.FinanceModels.*;
import br.com.pabelis.quickclock.domain.finance.FinanceRules;
import br.com.pabelis.quickclock.domain.shared.BusinessException;
import br.com.pabelis.quickclock.domain.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FinanceService {
    private final FinanceRepositoryPort repository;
    private final Clock clock;
    public FinanceService(FinanceRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }
    @Transactional(readOnly = true)
    public List<Card> cards() { return repository.cards(); }

    public Card saveCard(Long id, String name, int closingDay, int dueDay) {
        if (id != null) findCard(id);
        return repository.saveCard(new Card(id, name, closingDay, dueDay));
    }

    public Income receive(Long companyId, SourceType type, Long sourceId, LocalDate date) {
        if (type == null || sourceId == null) throw new BusinessException("Income source is required.");
        Source source = repository.lockSource(type, sourceId, companyId);
        var existing = repository.income(type, sourceId);
        if (existing.isPresent()) return existing.get();
        actualDate(date);
        if (date.isBefore(source.date())) throw new BusinessException("Income date cannot precede its source date.");
        return repository.insertIncome(source, date);
    }

    public void reverse(Long companyId, Long incomeId) {
        Income income = repository.income(incomeId, companyId)
            .orElseThrow(() -> new NotFoundException("Income not found."));
        repository.lockSource(income.sourceType(), income.sourceId(), companyId);
        repository.deleteIncome(incomeId);
    }

    @Transactional(readOnly = true)
    public List<Expense> expenses(YearMonth month) { return repository.expenses(month); }

    public List<Expense> createExpense(String description, LocalDate date, long amountCents, String category,
                                      PaymentMethod method, Long cardId, int count) {
        description = FinanceRules.text(description, 255);
        category = category(category);
        if (date == null || method == null) throw new BusinessException("Expense date and payment method are required.");
        if ((method == PaymentMethod.CREDIT_CARD) != (cardId != null))
            throw new BusinessException("Card is required only for credit card expenses.");
        List<Long> amounts = FinanceRules.split(amountCents, count);
        List<LocalDate> dates;
        if (cardId != null) {
            Card card = findCard(cardId);
            dates = FinanceRules.dueDates(date, card.closingDay(), card.dueDay(), count);
        } else {
            dates = new ArrayList<>();
            for (int i = 0; i < count; i++) dates.add(date.plusMonths(i));
        }
        String group = UUID.randomUUID().toString();
        List<Expense> result = new ArrayList<>();
        for (int i = 0; i < count; i++) result.add(repository.saveExpense(new Expense(null, dates.get(i), description,
            amounts.get(i), category, method, cardId, group, i + 1, count, false, null)));
        return List.copyOf(result);
    }

    public List<Expense> editExpense(Long id, EditScope scope, String description, long amountCents, String category) {
        description = FinanceRules.text(description, 255);
        FinanceRules.money(amountCents);
        category = category(category);
        Expense anchor = findExpense(id);
        List<Expense> group = repository.lockGroup(anchor.installmentGroupId());
        anchor = group.stream().filter(e -> e.id().equals(id)).findFirst()
            .orElseThrow(() -> new NotFoundException("Expense not found."));
        if (anchor.paid()) throw new BusinessException("Paid installments cannot be edited. Select an unpaid installment.");
        if (scope == null) throw new BusinessException("Edit scope is required.");
        List<Expense> changed = new ArrayList<>();
        for (Expense expense : group) {
            if (!expense.paid() && (scope == EditScope.UNPAID || expense.id().equals(id)))
                changed.add(repository.saveExpense(expense.edit(description, amountCents, category)));
        }
        return List.copyOf(changed);
    }

    public Expense payExpense(Long id, LocalDate date) {
        actualDate(date);
        Expense anchor = findExpense(id);
        Expense expense = repository.lockGroup(anchor.installmentGroupId()).stream()
            .filter(e -> e.id().equals(id)).findFirst().orElseThrow(() -> new NotFoundException("Expense not found."));
        if (expense.paid()) return expense;
        return repository.saveExpense(expense.pay(date));
    }

    public void deleteExpense(Long id) {
        Expense anchor = findExpense(id);
        Expense expense = repository.lockGroup(anchor.installmentGroupId()).stream()
            .filter(e -> e.id().equals(id)).findFirst().orElseThrow(() -> new NotFoundException("Expense not found."));
        if (expense.paid()) throw new BusinessException("Paid installments cannot be deleted.");
        repository.deleteExpense(id);
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Summary summary(YearMonth month) {
        List<Income> incomes = repository.incomes(month);
        List<Expense> expenses = repository.expenses(month);
        List<Source> pending = repository.pending(month);
        List<Expense> paid = repository.paidExpenses(month);
        long income = sum(incomes.stream().map(Income::amountCents).toList());
        long expense = sum(paid.stream().map(Expense::amountCents).toList());
        long expectedIncome = sum(pending.stream().map(Source::amountCents).toList());
        long expectedExpense = sum(expenses.stream().filter(e -> !e.paid()).map(Expense::amountCents).toList());
        long balance = Math.subtractExact(income, expense);
        long projected = Math.subtractExact(Math.addExact(balance, expectedIncome), expectedExpense);
        return new Summary(month, income, expense, expectedIncome, expectedExpense, balance, projected, incomes, expenses, pending, paid);
    }

    private long sum(List<Long> values) { return values.stream().reduce(0L, Math::addExact); }
    private Card findCard(Long id) { return repository.card(id).orElseThrow(() -> new NotFoundException("Card not found.")); }
    private Expense findExpense(Long id) { return repository.expense(id).orElseThrow(() -> new NotFoundException("Expense not found.")); }
    private String category(String category) {
        return category == null || category.isBlank() ? null : FinanceRules.text(category, 120);
    }
    private void actualDate(LocalDate date) {
        if (date == null || date.isAfter(LocalDate.now(clock))) throw new BusinessException("Actual payment date is required and cannot be in the future.");
    }
}
