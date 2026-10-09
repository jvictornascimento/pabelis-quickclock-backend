package br.com.pabelis.quickclock.adapters.in.web.additionalservice;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

final class AdditionalServiceRequests {

    private AdditionalServiceRequests() {
    }

    record SaveAdditionalServiceRequest(
        @NotNull(message = "is required")
        LocalDate date,

        @NotBlank(message = "is required")
        @Size(max = 255, message = "must have at most 255 characters")
        String description,

        @Min(value = 0, message = "must be zero or greater")
        long amountCents
    ) {
    }
}
