package br.com.pabelis.quickclock.adapters.in.web.estimate;

import br.com.pabelis.quickclock.application.estimate.EstimateService;
import br.com.pabelis.quickclock.domain.estimate.EstimateHistory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import br.com.pabelis.quickclock.domain.shared.BusinessException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/companies/{companyId}/estimates")
public class EstimateController {
    private final EstimateService service;
    public EstimateController(EstimateService service) { this.service = service; }

    public record SaveEstimateRequest(
        @NotNull LocalDate date,
        @NotBlank @Size(max = 255) String description,
        @NotNull @Min(0)
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = WholeCentsDeserializer.class)
        Long amountCents
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EstimateResponse create(@PathVariable Long companyId, @Valid @RequestBody SaveEstimateRequest request) {
        return EstimateResponse.from(service.create(companyId, request.date(), request.description(), request.amountCents()));
    }

    @GetMapping
    List<EstimateResponse> list(@PathVariable Long companyId, @RequestParam(required = false) String month) {
        YearMonth parsed = null;
        if (month != null) {
            try { parsed = YearMonth.parse(month); }
            catch (DateTimeParseException exception) { throw new BusinessException("Month must use YYYY-MM format."); }
        }
        return service.list(companyId, parsed).stream().map(EstimateResponse::from).toList();
    }

    @GetMapping("/{estimateId}")
    EstimateResponse get(@PathVariable Long companyId, @PathVariable Long estimateId) {
        return EstimateResponse.from(service.get(companyId, estimateId));
    }

    @PutMapping("/{estimateId}")
    EstimateResponse update(@PathVariable Long companyId, @PathVariable Long estimateId,
        @Valid @RequestBody SaveEstimateRequest request) {
        return EstimateResponse.from(service.update(companyId, estimateId, request.date(), request.description(), request.amountCents()));
    }

    @DeleteMapping("/{estimateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long companyId, @PathVariable Long estimateId) { service.delete(companyId, estimateId); }

    @PostMapping("/{estimateId}/approve")
    EstimateResponse approve(@PathVariable Long companyId, @PathVariable Long estimateId) {
        return EstimateResponse.from(service.approve(companyId, estimateId));
    }

    @GetMapping("/{estimateId}/history")
    List<EstimateHistory> history(@PathVariable Long companyId, @PathVariable Long estimateId) {
        return service.history(companyId, estimateId);
    }
}
