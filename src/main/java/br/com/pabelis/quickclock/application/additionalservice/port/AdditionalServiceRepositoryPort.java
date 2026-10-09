package br.com.pabelis.quickclock.application.additionalservice.port;

import br.com.pabelis.quickclock.domain.additionalservice.AdditionalService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdditionalServiceRepositoryPort {

    AdditionalService save(AdditionalService service);

    Optional<AdditionalService> findByIdAndCompanyId(Long id, Long companyId);

    List<AdditionalService> findByCompanyIdAndDateBetween(Long companyId, LocalDate startDate, LocalDate endDate);

    void delete(AdditionalService service);
}
