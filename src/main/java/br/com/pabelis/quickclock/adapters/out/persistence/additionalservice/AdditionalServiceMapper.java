package br.com.pabelis.quickclock.adapters.out.persistence.additionalservice;

import br.com.pabelis.quickclock.domain.additionalservice.AdditionalService;

final class AdditionalServiceMapper {

    private AdditionalServiceMapper() {
    }

    static AdditionalService toDomain(AdditionalServiceEntity entity) {
        return new AdditionalService(
            entity.getId(),
            entity.getCompanyId(),
            entity.getDate(),
            entity.getDescription(),
            entity.getAmountCents(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    static AdditionalServiceEntity toEntity(AdditionalService service) {
        return new AdditionalServiceEntity(
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
