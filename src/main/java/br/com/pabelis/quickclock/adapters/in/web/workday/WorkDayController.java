package br.com.pabelis.quickclock.adapters.in.web.workday;

import br.com.pabelis.quickclock.application.workday.WorkDayService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static br.com.pabelis.quickclock.adapters.in.web.workday.WorkDayRequests.SaveWorkDayRequest;
import static br.com.pabelis.quickclock.adapters.in.web.workday.WorkDayResponses.WorkDayResponse;

@RestController
@RequestMapping("/api/companies/{companyId}/work-days")
public class WorkDayController {

    private final WorkDayService workDayService;

    public WorkDayController(WorkDayService workDayService) {
        this.workDayService = workDayService;
    }

    @GetMapping
    List<WorkDayResponse> search(
        @PathVariable Long companyId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        if (date != null) {
            return workDayService.searchByDate(companyId, date).stream()
                .map(WorkDayResponse::from)
                .toList();
        }

        YearMonth selectedMonth = month == null ? YearMonth.now() : month;
        return workDayService.searchByMonth(companyId, selectedMonth).stream()
            .map(WorkDayResponse::from)
            .toList();
    }

    @GetMapping("/{date}")
    WorkDayResponse get(
        @PathVariable Long companyId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return WorkDayResponse.from(workDayService.get(companyId, date));
    }

    @PutMapping("/{date}")
    WorkDayResponse save(
        @PathVariable Long companyId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @Valid @RequestBody SaveWorkDayRequest request
    ) {
        return WorkDayResponse.from(
            workDayService.save(
                companyId,
                date,
                request.workedBeforeLunch(),
                request.workedAfterLunch()
            )
        );
    }

    @PatchMapping("/{date}/before-lunch/toggle")
    WorkDayResponse toggleBeforeLunch(
        @PathVariable Long companyId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return WorkDayResponse.from(workDayService.toggleBeforeLunch(companyId, date));
    }

    @PatchMapping("/{date}/after-lunch/toggle")
    WorkDayResponse toggleAfterLunch(
        @PathVariable Long companyId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return WorkDayResponse.from(workDayService.toggleAfterLunch(companyId, date));
    }
}
