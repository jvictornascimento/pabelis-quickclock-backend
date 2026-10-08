package br.com.pabelis.quickclock.adapters.in.web.company;

import br.com.pabelis.quickclock.application.company.CompanyService;
import br.com.pabelis.quickclock.domain.company.WeekdaySettings;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static br.com.pabelis.quickclock.adapters.in.web.company.CompanyRequests.SaveCompanyRequest;
import static br.com.pabelis.quickclock.adapters.in.web.company.CompanyRequests.SaveCompanySettingsRequest;
import static br.com.pabelis.quickclock.adapters.in.web.company.CompanyRequests.WeekdaySettingsRequest;
import static br.com.pabelis.quickclock.adapters.in.web.company.CompanyResponses.CompanyResponse;
import static br.com.pabelis.quickclock.adapters.in.web.company.CompanyResponses.CompanySettingsResponse;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CompanyResponse create(@Valid @RequestBody SaveCompanyRequest request) {
        return CompanyResponse.from(companyService.create(request.name()));
    }

    @GetMapping
    List<CompanyResponse> list() {
        return companyService.list().stream()
            .map(CompanyResponse::from)
            .toList();
    }

    @GetMapping("/{companyId}")
    CompanyResponse get(@PathVariable Long companyId) {
        return CompanyResponse.from(companyService.get(companyId));
    }

    @PutMapping("/{companyId}")
    CompanyResponse update(
        @PathVariable Long companyId,
        @Valid @RequestBody SaveCompanyRequest request
    ) {
        return CompanyResponse.from(companyService.update(companyId, request.name()));
    }

    @DeleteMapping("/{companyId}")
    CompanyResponse deactivate(@PathVariable Long companyId) {
        return CompanyResponse.from(companyService.deactivate(companyId));
    }

    @GetMapping("/{companyId}/settings")
    CompanySettingsResponse getSettings(@PathVariable Long companyId) {
        return CompanySettingsResponse.from(companyService.getSettings(companyId));
    }

    @PutMapping("/{companyId}/settings")
    CompanySettingsResponse updateSettings(
        @PathVariable Long companyId,
        @Valid @RequestBody SaveCompanySettingsRequest request
    ) {
        return CompanySettingsResponse.from(
            companyService.updateSettings(
                companyId,
                request.halfDayValueCents(),
                toDomain(request.weekdays())
            )
        );
    }

    private WeekdaySettings toDomain(WeekdaySettingsRequest request) {
        return new WeekdaySettings(
            request.mondayActive(),
            request.tuesdayActive(),
            request.wednesdayActive(),
            request.thursdayActive(),
            request.fridayActive(),
            request.saturdayActive(),
            request.sundayActive()
        );
    }
}
