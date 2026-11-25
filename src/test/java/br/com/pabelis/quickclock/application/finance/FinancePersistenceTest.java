package br.com.pabelis.quickclock.application.finance;

import br.com.pabelis.quickclock.application.finance.port.FinanceSourceGuard;
import br.com.pabelis.quickclock.application.additionalservice.AdditionalServiceService;
import br.com.pabelis.quickclock.domain.finance.FinanceModels.*;
import br.com.pabelis.quickclock.domain.shared.BusinessException;
import br.com.pabelis.quickclock.domain.shared.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FinancePersistenceTest {
    @Autowired FinanceService finance;
    @Autowired FinanceSourceGuard guard;
    @Autowired AdditionalServiceService additionalServices;
    @Autowired JdbcTemplate jdbc;

    private Long company() {
        jdbc.update("INSERT INTO company(name, active, created_at, updated_at) VALUES ('Finance test',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        return jdbc.queryForObject("SELECT MAX(id) FROM company", Long.class);
    }
    private Long service(Long company, long amount) {
        jdbc.update("INSERT INTO additional_service(company_id,service_date,description,amount_cents,created_at,updated_at) VALUES (?,DATE '2026-01-05','Repair',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", company, amount);
        return jdbc.queryForObject("SELECT MAX(id) FROM additional_service WHERE company_id=?", Long.class, company);
    }
    private List<Expense> installments() {
        Card card = finance.saveCard(null, "Visa", 20, 5);
        return finance.createExpense("Purchase", LocalDate.of(2026,1,10), 100, "Tools", PaymentMethod.CREDIT_CARD, card.id(), 3);
    }
    @Test void receivesSnapshotExactlyOnceAndReversesToPendingWithoutHistory() {
        Long company = company(), source = service(company, 100);
        Income first = finance.receive(company, SourceType.ADDITIONAL_SERVICE, source, LocalDate.of(2026,1,6));
        Income duplicate = finance.receive(company, SourceType.ADDITIONAL_SERVICE, source, LocalDate.of(2026,1,7));
        assertThat(duplicate).isEqualTo(first);
        assertThat(first.amountCents()).isEqualTo(100);
        assertThat(finance.summary(YearMonth.of(2026,1)).pendingIncome()).noneMatch(s -> s.sourceId().equals(source) && s.companyId().equals(company));
        finance.reverse(company, first.id());
        assertThat(finance.summary(YearMonth.of(2026,1)).pendingIncome()).anyMatch(s -> s.sourceId().equals(source) && s.companyId().equals(company));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM financial_income WHERE id=?", Long.class, first.id())).isZero();
    }
    @Test void rejectsCrossCompanyAndPreSourceReceipts() {
        Long company = company(), other = company(), source = service(company, 20);
        assertThatThrownBy(() -> finance.receive(other, SourceType.ADDITIONAL_SERVICE, source, LocalDate.of(2026,1,6))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> finance.receive(company, SourceType.ADDITIONAL_SERVICE, source, LocalDate.of(2026,1,4))).isInstanceOf(BusinessException.class);
    }
    @Test void protectsReceivedAdditionalServiceUntilReversed() {
        Long company = company(), source = service(company, 20);
        Income income = finance.receive(company, SourceType.ADDITIONAL_SERVICE, source, LocalDate.of(2026,1,6));
        assertThatThrownBy(() -> guard.lockAndAssertUnreceived("ADDITIONAL_SERVICE",source,company)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> additionalServices.update(company,source,LocalDate.of(2026,1,5),"Changed",30)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> additionalServices.delete(company,source)).isInstanceOf(BusinessException.class);
        finance.reverse(company, income.id());
        assertThatCode(() -> guard.lockAndAssertUnreceived("ADDITIONAL_SERVICE",source,company)).doesNotThrowAnyException();
    }
    @Test void preservesPaidInstallmentsWhenEditingAllOpenOnes() {
        List<Expense> expenses = installments();
        finance.payExpense(expenses.getFirst().id(),LocalDate.of(2026,2,5));
        assertThat(finance.editExpense(expenses.get(1).id(), EditScope.UNPAID,"Updated",50,null)).hasSize(2);
        Expense paid = finance.expenses(YearMonth.of(2026,2)).stream().filter(e -> e.id().equals(expenses.getFirst().id())).findFirst().orElseThrow();
        assertThat(paid.amountCents()).isEqualTo(34);
        assertThat(paid.description()).isEqualTo("Purchase");
        assertThat(paid.paid()).isTrue();
        assertThatThrownBy(() -> finance.deleteExpense(paid.id())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> finance.editExpense(paid.id(),EditScope.ONE,"No",1,null)).isInstanceOf(BusinessException.class);
    }
    @Test void payingTwiceDoesNotDoubleExpenseOrChangePaymentDate() {
        Expense expense = installments().getFirst();
        finance.payExpense(expense.id(),LocalDate.of(2026,1,31));
        assertThat(finance.payExpense(expense.id(),LocalDate.of(2026,2,1)).paidAt()).isEqualTo(LocalDate.of(2026,1,31));
        assertThat(finance.summary(YearMonth.of(2026,1)).expenseCents()).isEqualTo(34);
        assertThat(finance.summary(YearMonth.of(2026,2)).expenseCents()).isZero();
        assertThat(finance.summary(YearMonth.of(2026,2)).expectedExpenseCents()).isZero();
    }
    @Test void dashboardIncludesAllCompaniesAndOnlyApprovedEstimatesAndMarkedHistoricalPeriods() {
        Long one = company(), two = company();
        Long received = service(one,100);
        service(two,200);
        jdbc.update("INSERT INTO work_day(company_id,work_date,worked_before_lunch,worked_after_lunch,before_lunch_value_cents,after_lunch_value_cents,created_at,updated_at) VALUES (?,DATE '2026-01-02',true,false,50,999,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", one);
        jdbc.update("INSERT INTO estimate(company_id,estimate_date,description,amount_cents,status,approved_at,created_at,updated_at) VALUES (?,DATE '2025-12-01','Approved',75,'APPROVED',TIMESTAMP '2026-01-04 12:00:00',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",two);
        jdbc.update("INSERT INTO estimate(company_id,estimate_date,description,amount_cents,status,created_at,updated_at) VALUES (?,DATE '2026-01-04','Draft',999,'DRAFT',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",two);
        finance.receive(one,SourceType.ADDITIONAL_SERVICE,received,LocalDate.of(2026,1,6));
        Summary summary = finance.summary(YearMonth.of(2026,1));
        assertThat(summary.incomeCents()).isEqualTo(100);
        assertThat(summary.expectedIncomeCents()).isEqualTo(325);
        assertThat(summary.projectedBalanceCents()).isEqualTo(425);
        assertThat(summary.pendingIncome()).hasSize(3);
        assertThat(summary.pendingIncome()).anyMatch(s -> s.sourceType() == SourceType.WORK_REPORT && s.description().equals("Trabalho em 2026-01-02"));
    }
    @Test void approvedEstimateUsesApprovalDateAndCannotBeReceivedBeforeIt() {
        Long company = company();
        jdbc.update("INSERT INTO estimate(company_id,estimate_date,description,amount_cents,status,approved_at,created_at,updated_at) VALUES (?,DATE '2025-12-01','Approved',75,'APPROVED',TIMESTAMP '2026-01-04 12:00:00',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",company);
        Long id = jdbc.queryForObject("SELECT MAX(id) FROM estimate WHERE company_id=?",Long.class,company);
        assertThatThrownBy(() -> finance.receive(company,SourceType.APPROVED_ESTIMATE,id,LocalDate.of(2026,1,3))).isInstanceOf(BusinessException.class);
        assertThat(finance.receive(company,SourceType.APPROVED_ESTIMATE,id,LocalDate.of(2026,1,4)).amountCents()).isEqualTo(75);
    }
    @Test void editingCardDoesNotMoveExistingInstallmentsAndUnpaidDeleteIsSingle() {
        List<Expense> expenses = installments();
        finance.saveCard(expenses.getFirst().cardId(),"Changed",15,28);
        assertThat(finance.expenses(YearMonth.of(2026,2))).anyMatch(e -> e.id().equals(expenses.getFirst().id()) && e.date().equals(LocalDate.of(2026,2,5)));
        finance.deleteExpense(expenses.get(1).id());
        assertThat(finance.expenses(YearMonth.of(2026,3))).noneMatch(e -> e.id().equals(expenses.get(1).id()));
        assertThat(finance.expenses(YearMonth.of(2026,4))).anyMatch(e -> e.id().equals(expenses.get(2).id()));
    }
    @Test void cardIsRequiredOnlyForCreditCard() {
        assertThatThrownBy(() -> finance.createExpense("Bad",LocalDate.of(2026,1,1),100,null,PaymentMethod.CREDIT_CARD,null,1)).isInstanceOf(BusinessException.class);
        Card card = finance.saveCard(null,"Visa",20,5);
        assertThatThrownBy(() -> finance.createExpense("Bad",LocalDate.of(2026,1,1),100,null,PaymentMethod.PIX,card.id(),1)).isInstanceOf(BusinessException.class);
    }
    @Test void databaseRejectsDuplicateSourceEvenOutsideTheApplication() {
        Long company = company(), source = service(company,10);
        finance.receive(company,SourceType.ADDITIONAL_SERVICE,source,LocalDate.of(2026,1,6));
        assertThatThrownBy(() -> jdbc.update("INSERT INTO financial_income(company_id,source_type,source_id,income_date,description,amount_cents,paid_at,created_at) VALUES (?,'ADDITIONAL_SERVICE',?,DATE '2026-01-06','Duplicate',10,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",company,source))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
