package br.com.pabelis.quickclock.application.additionalservice;

import br.com.pabelis.quickclock.application.additionalservice.port.AdditionalServiceRepositoryPort;
import br.com.pabelis.quickclock.application.company.port.CompanyRepositoryPort;
import br.com.pabelis.quickclock.domain.additionalservice.AdditionalService;
import br.com.pabelis.quickclock.domain.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class AdditionalServiceService {

    private final AdditionalServiceRepositoryPort serviceRepository;
    private final CompanyRepositoryPort companyRepository;
    private final Clock clock;

    public AdditionalServiceService(
        AdditionalServiceRepositoryPort serviceRepository,
        CompanyRepositoryPort companyRepository,
        Clock clock
    ) {
        this.serviceRepository = serviceRepository;
        this.companyRepository = companyRepository;
        this.clock = clock;
    }

    @Transactional
    public AdditionalService create(Long companyId, LocalDate date, String description, long amountCents) {
        ensureCompanyExists(companyId);
        return serviceRepository.save(
            AdditionalService.create(companyId, date, description, amountCents, now())
        );
    }

    public AdditionalService get(Long companyId, Long serviceId) {
        ensureCompanyExists(companyId);
        return findService(companyId, serviceId);
    }

    public List<AdditionalService> listByMonth(Long companyId, YearMonth month) {
        ensureCompanyExists(companyId);
        return serviceRepository.findByCompanyIdAndDateBetween(
            companyId,
            month.atDay(1),
            month.atEndOfMonth()
        );
    }

    @Transactional
    public AdditionalService update(
        Long companyId,
        Long serviceId,
        LocalDate date,
        String description,
        long amountCents
    ) {
        ensureCompanyExists(companyId);
        AdditionalService service = findService(companyId, serviceId);
        return serviceRepository.save(service.update(date, description, amountCents, now()));
    }

    @Transactional
    public void delete(Long companyId, Long serviceId) {
        ensureCompanyExists(companyId);
        serviceRepository.delete(findService(companyId, serviceId));
    }

    private AdditionalService findService(Long companyId, Long serviceId) {
        return serviceRepository.findByIdAndCompanyId(serviceId, companyId)
            .orElseThrow(() -> new NotFoundException("Additional service not found."));
    }

    private void ensureCompanyExists(Long companyId) {
        companyRepository.findById(companyId)
            .orElseThrow(() -> new NotFoundException("Company not found."));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
