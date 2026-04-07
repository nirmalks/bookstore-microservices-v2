package com.nirmalks.bookstore.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;

import exceptions.ServiceUnavailableException;
import reactor.test.StepVerifier;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FallbackControllerTest {

	private final FallbackController controller = new FallbackController();

	@Test
	void user_auth_fallback_should_return_message_for_get() {
		ServerHttpRequest request = MockServerHttpRequest.get("/fallback/userauth").build();

		StepVerifier.create(controller.userAuthFallback(request))
			.expectNext("User Auth Service is unavailable. Please try again later.")
			.verifyComplete();
	}

	@Test
	void catalog_fallback_should_return_message_for_get() {
		ServerHttpRequest request = MockServerHttpRequest.get("/fallback/catalog").build();

		StepVerifier.create(controller.catalogFallback(request))
			.expectNext("Catalog Service is unavailable. Please try again later.")
			.verifyComplete();
	}

	@Test
	void checkout_fallback_should_fail_for_non_get_requests() {
		ServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.POST, "/fallback/checkout").build();

		StepVerifier.create(controller.checkoutFallback(request)).expectErrorSatisfies(error -> {
			assertThat(error).isInstanceOf(ServiceUnavailableException.class);
			assertThat(error.getMessage()).isEqualTo("Service unavailable and no fallback for non-GET methods.");
		}).verify();
	}

	@Test
	void auth_fallback_should_fail_for_non_get_requests() {
		ServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.DELETE, "/fallback/auth").build();

		StepVerifier.create(controller.authFallback(request)).expectError(ServiceUnavailableException.class).verify();
	}

}
