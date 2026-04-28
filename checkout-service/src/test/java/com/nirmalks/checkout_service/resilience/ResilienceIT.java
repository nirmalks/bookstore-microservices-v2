package com.nirmalks.checkout_service.resilience;

import com.nirmalks.checkout_service.AbstractIntegrationTest;
import com.nirmalks.checkout_service.order.api.DirectOrderRequest;
import com.nirmalks.checkout_service.order.api.OrderItemRequest;
import com.nirmalks.checkout_service.order.dto.AddressRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Resilience patterns (Circuit Breaker, Fallbacks).
 */
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = { "user-service.base-url=http://localhost:${wiremock.server.port}",
		"catalog-service.base-url=http://localhost:${wiremock.server.port}",
		"resilience4j.circuitbreaker.instances.catalogService.sliding-window-size=5",
		"resilience4j.circuitbreaker.instances.catalogService.failure-rate-threshold=50",
		"resilience4j.circuitbreaker.instances.catalogService.wait-duration-in-open-state=10s",
		"resilience4j.retry.instances.catalogService.max-attempts=1" })
class ResilienceIT extends AbstractIntegrationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	@DisplayName("Should trigger fallback when downstream service is failing")
	void shouldOpenCircuitBreakerAndInvokeFallback() {
		// 1. Setup WireMock to return success for User Service but 500 errors for Catalog
		// Service
		stubFor(get(urlMatching("/api/v1/users/.*"))
			.willReturn(aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.withBody("{\"id\": 1, \"username\": \"testuser\", \"email\": \"test@example.com\"}")));

		stubFor(get(urlMatching("/api/v1/books/.*")).willReturn(aResponse().withStatus(500)));

		// 2. Make multiple requests to /api/v1/orders/direct to trigger Circuit Breaker
		OrderItemRequest item = new OrderItemRequest(1L, 1, 10.0);

		AddressRequest address = new AddressRequest("Metropolis", "NY", "USA", "10001", true, "123 Main St");
		DirectOrderRequest request = new DirectOrderRequest(1L, List.of(item), address);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<DirectOrderRequest> entity = new HttpEntity<>(request, headers);

		// Trip the circuit (sliding window size 5, threshold 50%)
		for (int i = 0; i < 6; i++) {
			ResponseEntity<Object> response = restTemplate.postForEntity("/api/v1/orders/direct", entity, Object.class);
			// 503 Service Unavailable is returned by GlobalExceptionHandler when fallback
			// throws ServiceUnavailableException
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		}

		// 3. Verify that the response still comes from the fallback method and circuit is
		// OPEN
		// (Resilience4j will not even call the external service if the circuit is open)
		ResponseEntity<Object> openCircuitResponse = restTemplate.postForEntity("/api/v1/orders/direct", entity,
				Object.class);
		assertThat(openCircuitResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

		// Ensure catalog service was not called for the last request
		verify(lessThan(7), getRequestedFor(urlMatching("/api/v1/books/.*")));
	}

}
