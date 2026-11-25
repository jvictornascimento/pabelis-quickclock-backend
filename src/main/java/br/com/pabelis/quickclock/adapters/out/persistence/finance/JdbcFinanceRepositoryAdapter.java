package br.com.pabelis.quickclock.adapters.out.persistence.finance;

import br.com.pabelis.quickclock.application.finance.port.FinanceRepositoryPort;
import br.com.pabelis.quickclock.application.finance.port.FinanceSourceGuard;
import br.com.pabelis.quickclock.domain.finance.FinanceModels.*;
import br.com.pabelis.quickclock.domain.shared.BusinessException;
import br.com.pabelis.quickclock.domain.shared.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcFinanceRepositoryAdapter implements FinanceRepositoryPort, FinanceSourceGuard {
    private final JdbcTemplate jdbc;
    public JdbcFinanceRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final String SOURCE_ROWS = """
        SELECT company_id, 'WORK_REPORT' AS source_type, id AS source_id, work_date AS source_date,
          CONCAT('Trabalho em ', CAST(work_date AS VARCHAR(10))) AS description,
          (CASE WHEN worked_before_lunch THEN COALESCE(before_lunch_value_cents,0) ELSE 0 END +
           CASE WHEN worked_after_lunch THEN COALESCE(after_lunch_value_cents,0) ELSE 0 END) AS amount_cents
        FROM work_day WHERE worked_before_lunch OR worked_after_lunch
        UNION ALL
        SELECT company_id, 'ADDITIONAL_SERVICE', id, service_date, description, amount_cents
        FROM additional_service
        UNION ALL
        SELECT company_id, 'APPROVED_ESTIMATE', id, CAST(approved_at AS DATE), description, amount_cents
        FROM estimate WHERE status = 'APPROVED' AND approved_at IS NOT NULL
        """;
    private static final String SOURCES = "SELECT s.*, c.name AS company_name FROM (" + SOURCE_ROWS + ") s JOIN company c ON c.id=s.company_id";
    private static final String INCOMES = "SELECT i.*, c.name AS company_name FROM financial_income i JOIN company c ON c.id=i.company_id";

    @Override public List<Card> cards() { return jdbc.query("SELECT * FROM credit_card ORDER BY id", CARD); }
    @Override public Optional<Card> card(Long id) {
        return jdbc.query("SELECT * FROM credit_card WHERE id=?",CARD,id).stream().findFirst();
    }
    @Override public Card saveCard(Card card) {
        Long id = card.id();
        if (id == null) id = insert("INSERT INTO credit_card(name,closing_day,due_day) VALUES (?,?,?)",card.name(),card.closingDay(),card.dueDay());
        else jdbc.update("UPDATE credit_card SET name=?,closing_day=?,due_day=? WHERE id=?",card.name(),card.closingDay(),card.dueDay(),id);
        return new Card(id,card.name(),card.closingDay(),card.dueDay());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Source lockSource(SourceType type, Long sourceId, Long companyId) {
        lockSourceRow(type,sourceId,companyId);
        return jdbc.query("SELECT * FROM (" + SOURCES + ") s WHERE source_type=? AND source_id=? AND company_id=?",
            SOURCE,type.name(),sourceId,companyId).stream().findFirst()
            .orElseThrow(() -> new BusinessException("Source has no receivable work or is not approved."));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockAndAssertUnreceived(String sourceType, Long sourceId, Long companyId) {
        SourceType type = SourceType.valueOf(sourceType);
        lockSourceRow(type,sourceId,companyId);
        if (income(type,sourceId).isPresent()) throw new BusinessException("Received sources cannot be changed or deleted. Reverse the income first.");
    }

    private void lockSourceRow(SourceType type, Long sourceId, Long companyId) {
        String table = switch (type) {
            case WORK_REPORT -> "work_day";
            case ADDITIONAL_SERVICE -> "additional_service";
            case APPROVED_ESTIMATE -> "estimate";
        };
        if (jdbc.queryForList("SELECT id FROM " + table + " WHERE id=? AND company_id=? FOR UPDATE",Long.class,sourceId,companyId).isEmpty())
            throw new NotFoundException("Income source not found in company.");
    }

    @Override public Optional<Income> income(SourceType type, Long sourceId) {
        return jdbc.query(INCOMES + " WHERE i.source_type=? AND i.source_id=?",INCOME,type.name(),sourceId).stream().findFirst();
    }
    @Override public Optional<Income> income(Long incomeId, Long companyId) {
        return jdbc.query(INCOMES + " WHERE i.id=? AND i.company_id=?",INCOME,incomeId,companyId).stream().findFirst();
    }
    @Override public Income insertIncome(Source source, LocalDate date) {
        Long id = insert("""
            INSERT INTO financial_income(company_id,source_type,source_id,income_date,description,amount_cents,paid_at,created_at)
            VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
            """,source.companyId(),source.sourceType().name(),source.sourceId(),date,source.description(),source.amountCents());
        return new Income(id,source.companyId(),source.companyName(),source.sourceType(),source.sourceId(),date,source.description(),source.amountCents());
    }
    @Override public void deleteIncome(Long id) { jdbc.update("DELETE FROM financial_income WHERE id=?",id); }
    @Override public List<Income> incomes(YearMonth month) {
        return jdbc.query(INCOMES + " WHERE i.income_date>=? AND i.income_date<? ORDER BY i.income_date,i.id",INCOME,month.atDay(1),month.plusMonths(1).atDay(1));
    }
    @Override public List<Source> pending(YearMonth month) {
        return jdbc.query("SELECT * FROM (" + SOURCES + """
            ) s WHERE source_date>=? AND source_date<? AND NOT EXISTS (
              SELECT 1 FROM financial_income i WHERE i.source_type=s.source_type AND i.source_id=s.source_id)
            ORDER BY source_date, company_id, source_type, source_id
            """,SOURCE,month.atDay(1),month.plusMonths(1).atDay(1));
    }

    @Override public Expense saveExpense(Expense expense) {
        Long id = expense.id();
        if (id == null) id = insert("""
            INSERT INTO expense(expense_date,description,amount_cents,category,payment_method,card_id,
            installment_group_id,installment_number,installment_total,paid_at,created_at,updated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
            """,expense.date(),expense.description(),expense.amountCents(),expense.category(),expense.paymentMethod().name(),
            expense.cardId(),expense.installmentGroupId(),expense.installmentNumber(),expense.installmentTotal(),expense.paidAt());
        else jdbc.update("UPDATE expense SET description=?,amount_cents=?,category=?,paid_at=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
            expense.description(),expense.amountCents(),expense.category(),expense.paidAt(),id);
        return expense(id).orElseThrow();
    }
    @Override public Optional<Expense> expense(Long id) { return jdbc.query("SELECT * FROM expense WHERE id=?",EXPENSE,id).stream().findFirst(); }
    @Override public List<Expense> lockGroup(String group) {
        return jdbc.query("SELECT * FROM expense WHERE installment_group_id=? ORDER BY id FOR UPDATE",EXPENSE,group);
    }
    @Override public List<Expense> expenses(YearMonth month) {
        return jdbc.query("SELECT * FROM expense WHERE expense_date>=? AND expense_date<? ORDER BY expense_date,id",EXPENSE,month.atDay(1),month.plusMonths(1).atDay(1));
    }
    @Override public List<Expense> paidExpenses(YearMonth month) {
        return jdbc.query("SELECT * FROM expense WHERE paid_at>=? AND paid_at<? ORDER BY paid_at,id",EXPENSE,month.atDay(1),month.plusMonths(1).atDay(1));
    }
    @Override public void deleteExpense(Long id) { jdbc.update("DELETE FROM expense WHERE id=?",id); }

    private Long insert(String sql, Object... args) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(sql,new String[]{"id"});
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1,args[i]);
            return statement;
        },key);
        return key.getKey().longValue();
    }
    private static LocalDate date(ResultSet rs, String name) throws SQLException { return rs.getObject(name,LocalDate.class); }
    private static final RowMapper<Card> CARD = (rs,n) -> new Card(rs.getLong("id"),rs.getString("name"),rs.getInt("closing_day"),rs.getInt("due_day"));
    private static final RowMapper<Source> SOURCE = (rs,n) -> new Source(rs.getLong("company_id"),rs.getString("company_name"),SourceType.valueOf(rs.getString("source_type")),rs.getLong("source_id"),date(rs,"source_date"),rs.getString("description"),rs.getLong("amount_cents"));
    private static final RowMapper<Income> INCOME = (rs,n) -> new Income(rs.getLong("id"),rs.getLong("company_id"),rs.getString("company_name"),SourceType.valueOf(rs.getString("source_type")),rs.getLong("source_id"),date(rs,"income_date"),rs.getString("description"),rs.getLong("amount_cents"));
    private static final RowMapper<Expense> EXPENSE = (rs,n) -> new Expense(rs.getLong("id"),date(rs,"expense_date"),rs.getString("description"),rs.getLong("amount_cents"),rs.getString("category"),PaymentMethod.valueOf(rs.getString("payment_method")),rs.getObject("card_id",Long.class),rs.getString("installment_group_id"),rs.getInt("installment_number"),rs.getInt("installment_total"),date(rs,"paid_at") != null,date(rs,"paid_at"));
}
