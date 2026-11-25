package br.com.pabelis.quickclock.adapters.in.web.finance;

import br.com.pabelis.quickclock.application.finance.FinanceService;
import br.com.pabelis.quickclock.domain.finance.FinanceModels.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
public class FinanceController {
    private final FinanceService service;
    public FinanceController(FinanceService service) { this.service = service; }

    @GetMapping("/api/finance")
    Summary summary(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) { return service.summary(month); }

    @GetMapping("/api/cards")
    List<Card> cards() { return service.cards(); }

    @PostMapping("/api/cards")
    @ResponseStatus(HttpStatus.CREATED)
    Card createCard(@Valid @RequestBody CardRequest request) {
        return service.saveCard(null,request.name(),request.closingDay(),request.dueDay());
    }
    @PutMapping("/api/cards/{id}")
    Card updateCard(@PathVariable Long id, @Valid @RequestBody CardRequest request) {
        return service.saveCard(id,request.name(),request.closingDay(),request.dueDay());
    }
    @GetMapping("/api/expenses")
    List<Expense> expenses(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) { return service.expenses(month); }

    @PostMapping("/api/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    List<Expense> createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        return service.createExpense(request.description(),request.date(),request.amountCents(),request.category(),
            request.paymentMethod(),request.cardId(),request.installmentTotal() == null ? 1 : request.installmentTotal());
    }
    @PutMapping("/api/expenses/{id}")
    List<Expense> editExpense(@PathVariable Long id, @RequestParam EditScope scope, @Valid @RequestBody EditExpenseRequest request) {
        return service.editExpense(id,scope,request.description(),request.amountCents(),request.category());
    }
    @PostMapping("/api/expenses/{id}/pay")
    Expense payExpense(@PathVariable Long id, @Valid @RequestBody DateRequest request) { return service.payExpense(id,request.date()); }

    @DeleteMapping("/api/expenses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteExpense(@PathVariable Long id) { service.deleteExpense(id); }

    @PostMapping("/api/companies/{companyId}/incomes")
    @ResponseStatus(HttpStatus.CREATED)
    Income receive(@PathVariable Long companyId, @Valid @RequestBody ReceiveRequest request) {
        return service.receive(companyId,request.sourceType(),request.sourceId(),request.incomeDate());
    }
    @DeleteMapping("/api/companies/{companyId}/incomes/{incomeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reverse(@PathVariable Long companyId, @PathVariable Long incomeId) { service.reverse(companyId,incomeId); }

    public record CardRequest(@NotBlank @Size(max=120) String name,
        @JsonDeserialize(using=FinanceIntegerDeserializers.Count.class) @NotNull @Min(1) @Max(31) Integer closingDay,
        @JsonDeserialize(using=FinanceIntegerDeserializers.Count.class) @NotNull @Min(1) @Max(31) Integer dueDay) {}
    public record CreateExpenseRequest(@NotBlank @Size(max=255) String description, @NotNull LocalDate date,
        @JsonDeserialize(using=FinanceIntegerDeserializers.Cents.class) @NotNull @PositiveOrZero Long amountCents,
        @Size(max=120) String category, @NotNull PaymentMethod paymentMethod, @Positive Long cardId,
        @JsonDeserialize(using=FinanceIntegerDeserializers.Count.class) @Min(1) @Max(1200) Integer installmentTotal) {}
    public record EditExpenseRequest(@NotBlank @Size(max=255) String description,
        @JsonDeserialize(using=FinanceIntegerDeserializers.Cents.class) @NotNull @PositiveOrZero Long amountCents,
        @Size(max=120) String category) {}
    public record DateRequest(@NotNull LocalDate date) {}
    public record ReceiveRequest(@NotNull SourceType sourceType, @NotNull @Positive Long sourceId, @NotNull LocalDate incomeDate) {}
}
