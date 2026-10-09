package br.com.pabelis.quickclock.adapters.in.web.additionalservice;

import br.com.pabelis.quickclock.domain.additionalservice.AdditionalService;

import java.time.LocalDate;
import java.time.LocalDateTime;

final class AdditionalServiceResponses {

    private AdditionalServiceResponses() {
    }

    record AdditionalServiceResponse(
        Long id,
        Long companyId,
        LocalDate date,
        String description,
        long amountCents,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {

        static AdditionalServiceResponse from(AdditionalService service) {
            return new AdditionalServiceResponse(
                service.id(),
                service.companyId(),
                service.date(),
                service.description(),
                service.amountCents(),
                service.createdAt(),
                service.updatedAt()
            );
        }
    }
}
