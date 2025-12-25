package com.nirmalks.checkout_service.actuator;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Custom health indicator for external service dependencies Checks connectivity to
 * catalog-service and user-service
 */
@Component
public class ExternalServiceHealthIndicator implements HealthIndicator {

	private final WebClient catalogServiceWebClient;

	private final WebClient userServiceWebClient;

	public ExternalServiceHealthIndicator(@Qualifier("catalogServiceWebClient") WebClient catalogServiceWebClient,
			@Qualifier("userServiceWebClient") WebClient userServiceWebClient) {
		this.catalogServiceWebClient = catalogServiceWebClient;
		this.userServiceWebClient = userServiceWebClient;
	}

	@Override
	public Health health() {
		try {
			boolean catalogServiceUp = checkService(catalogServiceWebClient, "/actuator/health");
			boolean userServiceUp = checkService(userServiceWebClient, "/actuator/health");

			if (catalogServiceUp && userServiceUp) {
				return Health.up()
					.withDetail("catalog-service", "UP")
					.withDetail("user-service", "UP")
					.withDetail("status", "All external services healthy")
					.build();
			}
			else if (!catalogServiceUp && !userServiceUp) {
				return Health.down()
					.withDetail("catalog-service", "DOWN")
					.withDetail("user-service", "DOWN")
					.withDetail("status", "All external services down")
					.build();
			}
			else {
				return Health.up()
					.withDetail("catalog-service", catalogServiceUp ? "UP" : "DOWN")
					.withDetail("user-service", userServiceUp ? "UP" : "DOWN")
					.withDetail("status", "Partial degradation")
					.build();
			}
		}
		catch (Exception e) {
			return Health.down().withDetail("error", e.getMessage()).withException(e).build();
		}
	}

	private boolean checkService(WebClient webClient, String endpoint) {
		try {
			webClient.get().uri(endpoint).retrieve().bodyToMono(String.class).timeout(Duration.ofSeconds(3)).block();
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

}
