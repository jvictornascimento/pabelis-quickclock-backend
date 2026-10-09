package br.com.pabelis.quickclock.adapters.in.web.additionalservice;

import br.com.pabelis.quickclock.application.additionalservice.AdditionalServiceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

import static br.com.pabelis.quickclock.adapters.in.web.additionalservice.AdditionalServiceRequests.SaveAdditionalServiceRequest;
import static br.com.pabelis.quickclock.adapters.in.web.additionalservice.AdditionalServiceResponses.AdditionalServiceResponse;

@RestController
@RequestMapping("/api/companies/{companyId}/additional-services")
public class AdditionalServiceController {

    private final AdditionalServiceService service;

    public AdditionalServiceController(AdditionalServiceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AdditionalServiceResponse create(
        @PathVariable Long companyId,
        @Valid @RequestBody SaveAdditionalServiceRequest request
    ) {
        return AdditionalServiceResponse.from(
            service.create(companyId, request.date(), request.description(), request.amountCents())
        );
    }

    @GetMapping
    List<AdditionalServiceResponse> list(
        @PathVariable Long companyId,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return service.listByMonth(companyId, month).stream()
            .map(AdditionalServiceResponse::from)
            .toList();
    }

    @GetMapping("/{serviceId}")
    AdditionalServiceResponse get(@PathVariable Long companyId, @PathVariable Long serviceId) {
        return AdditionalServiceResponse.from(service.get(companyId, serviceId));
    }

    @PutMapping("/{serviceId}")
    AdditionalServiceResponse update(
        @PathVariable Long companyId,
        @PathVariable Long serviceId,
        @Valid @RequestBody SaveAdditionalServiceRequest request
    ) {
        return AdditionalServiceResponse.from(
            service.update(companyId, serviceId, request.date(), request.description(), request.amountCents())
        );
    }

    @DeleteMapping("/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long companyId, @PathVariable Long serviceId) {
        service.delete(companyId, serviceId);
    }
}
