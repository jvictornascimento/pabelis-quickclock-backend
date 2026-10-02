package br.com.pabelis.quickclock.adapters.in.web;

import br.com.pabelis.quickclock.application.health.HealthCheckUseCase;
import br.com.pabelis.quickclock.domain.health.HealthStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthCheckUseCase healthCheckUseCase;

    public HealthController(HealthCheckUseCase healthCheckUseCase) {
        this.healthCheckUseCase = healthCheckUseCase;
    }

    @GetMapping
    public HealthStatus check() {
        return healthCheckUseCase.check();
    }
}
