package br.com.pabelis.quickclock.application.health;

import br.com.pabelis.quickclock.domain.health.HealthStatus;
import org.springframework.stereotype.Service;

@Service
public class HealthCheckUseCase {

    public HealthStatus check() {
        return new HealthStatus("OK");
    }
}
