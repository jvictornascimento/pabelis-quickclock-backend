package br.com.pabelis.quickclock.adapters.in.web.report;

import br.com.pabelis.quickclock.application.report.MonthlyReportService;
import br.com.pabelis.quickclock.domain.shared.BusinessException;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/companies/{companyId}/reports")
public class MonthlyReportController {
    private final MonthlyReportService service;
    public MonthlyReportController(MonthlyReportService service) { this.service = service; }

    @GetMapping
    MonthlyReportResponse get(@PathVariable Long companyId, @RequestParam String month) {
        YearMonth parsed;
        try { parsed = YearMonth.parse(month); }
        catch (DateTimeParseException exception) { throw new BusinessException("Month must use YYYY-MM format."); }
        return MonthlyReportResponse.from(service.get(companyId, parsed));
    }
}
