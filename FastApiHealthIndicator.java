package io.fastbridger.health;

import io.fastbridger.autoconfigure.FastBridgeProperties;
import io.fastbridger.client.FastApiClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

import java.time.Instant;

/**
 * Spring Boot Actuator health indicator for the FastAPI connection.
 *
 * <p>Exposes {@code /actuator/health/fastapi} showing whether the
 * configured FastAPI service is reachable.
 *
 * <p>Probes FastAPI's {@code /health} or {@code /} endpoint (configurable).
 */
public class FastApiHealthIndicator implements HealthIndicator {

    private final FastApiClient client;
    private final FastBridgeProperties.FastApi props;

    public FastApiHealthIndicator(FastApiClient client, FastBridgeProperties.FastApi props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public Health health() {
        try {
            // Try FastAPI's /health endpoint first, fall back to root
            String body = client.rawClient()
                    .get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(3))
                    .onErrorResume(e -> client.rawClient()
                            .get()
                            .uri("/")
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(java.time.Duration.ofSeconds(3)))
                    .block();

            return Health.up()
                    .withDetail("fastapi.url", props.getBaseUrl())
                    .withDetail("fastapi.response", body != null ? "ok" : "empty")
                    .withDetail("checkedAt", Instant.now().toString())
                    .build();

        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("fastapi.url", props.getBaseUrl())
                    .withDetail("error", e.getMessage())
                    .withDetail("checkedAt", Instant.now().toString())
                    .build();
        }
    }
}
