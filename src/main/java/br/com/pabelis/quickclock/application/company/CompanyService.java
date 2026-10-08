package br.com.pabelis.quickclock.application.company;

import br.com.pabelis.quickclock.application.company.port.CompanyRepositoryPort;
import br.com.pabelis.quickclock.application.company.port.CompanySettingsRepositoryPort;
import br.com.pabelis.quickclock.domain.company.Company;
import br.com.pabelis.quickclock.domain.company.CompanySettings;
import br.com.pabelis.quickclock.domain.company.WeekdaySettings;
import br.com.pabelis.quickclock.domain.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepositoryPort companyRepository;
    private final CompanySettingsRepositoryPort settingsRepository;
    private final Clock clock;

    public CompanyService(
        CompanyRepositoryPort companyRepository,
        CompanySettingsRepositoryPort settingsRepository,
        Clock clock
    ) {
        this.companyRepository = companyRepository;
        this.settingsRepository = settingsRepository;
        this.clock = clock;
    }

    @Transactional
    public Company create(String name) {
        LocalDateTime now = now();
        Company company = companyRepository.save(Company.create(name, now));
        settingsRepository.save(CompanySettings.defaultFor(company.id(), now));
        return company;
    }

    public List<Company> list() {
        return companyRepository.findAll();
    }

    public Company get(Long companyId) {
        return findCompany(companyId);
    }

    @Transactional
    public Company update(Long companyId, String name) {
        Company company = findCompany(companyId);
        return companyRepository.save(company.rename(name, now()));
    }

    @Transactional
    public Company deactivate(Long companyId) {
        Company company = findCompany(companyId);
        return companyRepository.save(company.deactivate(now()));
    }

    public CompanySettings getSettings(Long companyId) {
        ensureCompanyExists(companyId);
        return settingsRepository.findByCompanyId(companyId)
            .orElseThrow(() -> new NotFoundException("Settings not found for company."));
    }

    @Transactional
    public CompanySettings updateSettings(
        Long companyId,
        long halfDayValueCents,
        WeekdaySettings weekdays
    ) {
        ensureCompanyExists(companyId);
        CompanySettings settings = settingsRepository.findByCompanyId(companyId)
            .orElseGet(() -> CompanySettings.defaultFor(companyId, now()));

        return settingsRepository.save(settings.update(halfDayValueCents, weekdays, now()));
    }

    private Company findCompany(Long companyId) {
        return companyRepository.findById(companyId)
            .orElseThrow(() -> new NotFoundException("Company not found."));
    }

    private void ensureCompanyExists(Long companyId) {
        findCompany(companyId);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
