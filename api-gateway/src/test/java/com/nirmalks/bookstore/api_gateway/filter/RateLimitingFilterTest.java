package com.nirmalks.bookstore.api_gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RateLimitingFilterTest {

	@Test
	void filter_should_allow_order_requests_with_order_limiter_headers() {
		RateLimiterRegistry registry = mock(RateLimiterRegistry.class);
		RateLimiter rateLimiter = mock(RateLimiter.class);
		RateLimiter.Metrics metrics = mock(RateLimiter.Metrics.class);
		RateLimitingFilter filter = new RateLimitingFilter(registry);
		MockServerWebExchange exchange = MockServerWebExchange
			.from(MockServerHttpRequest.get("/api/v1/orders/123").build());
		AtomicBoolean chainInvoked = new AtomicBoolean(false);
		GatewayFilterChain chain = serverWebExchange -> {
			chainInvoked.set(true);
			return Mono.empty();
		};

		when(registry.rateLimiter("ordersRateLimiter")).thenReturn(rateLimiter);
		when(rateLimiter.acquirePermission()).thenReturn(true);
		when(rateLimiter.getMetrics()).thenReturn(metrics);
		when(metrics.getAvailablePermissions()).thenReturn(2);
		when(rateLimiter.getRateLimiterConfig()).thenReturn(RateLimiterConfig.custom()
			.limitForPeriod(3)
			.limitRefreshPeriod(Duration.ofSeconds(10))
			.timeoutDuration(Duration.ZERO)
			.build());

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		verify(registry).rateLimiter("ordersRateLimiter");
		assertThat(chainInvoked).isTrue();
		assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("2");
		assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("3");
	}

	@Test
	void filter_should_return_too_many_requests_when_catalog_limiter_denies_permission() {
		RateLimiterRegistry registry = mock(RateLimiterRegistry.class);
		RateLimiter rateLimiter = mock(RateLimiter.class);
		RateLimitingFilter filter = new RateLimitingFilter(registry);
		MockServerWebExchange exchange = MockServerWebExchange
			.from(MockServerHttpRequest.get("/api/v1/books/abc").build());
		GatewayFilterChain chain = mock(GatewayFilterChain.class);

		when(registry.rateLimiter("catalogRateLimiter")).thenReturn(rateLimiter);
		when(rateLimiter.acquirePermission()).thenReturn(false);
		when(rateLimiter.getRateLimiterConfig()).thenReturn(RateLimiterConfig.custom()
			.limitForPeriod(10)
			.limitRefreshPeriod(Duration.ofSeconds(10))
			.timeoutDuration(Duration.ZERO)
			.build());

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		verify(registry).rateLimiter("catalogRateLimiter");
		verify(chain, never()).filter(exchange);
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limiter")).isEqualTo("catalogRateLimiter");
		assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("10");
	}

	@Test
	void should_use_global_limiter_for_other_paths() {
		RateLimiterRegistry registry = mock(RateLimiterRegistry.class);
		RateLimiter rateLimiter = mock(RateLimiter.class);
		RateLimiter.Metrics metrics = mock(RateLimiter.Metrics.class);
		RateLimitingFilter filter = new RateLimitingFilter(registry);
		MockServerWebExchange exchange = MockServerWebExchange
			.from(MockServerHttpRequest.get("/actuator/health").build());

		when(registry.rateLimiter("globalRateLimiter")).thenReturn(rateLimiter);
		when(rateLimiter.acquirePermission()).thenReturn(true);
		when(rateLimiter.getMetrics()).thenReturn(metrics);
		when(metrics.getAvailablePermissions()).thenReturn(4);
		when(rateLimiter.getRateLimiterConfig()).thenReturn(RateLimiterConfig.custom()
			.limitForPeriod(5)
			.limitRefreshPeriod(Duration.ofSeconds(10))
			.timeoutDuration(Duration.ZERO)
			.build());

		StepVerifier.create(filter.filter(exchange, serverWebExchange -> Mono.empty())).verifyComplete();

		verify(registry).rateLimiter("globalRateLimiter");
		assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("5");
	}

	@Test
	void get_order_should_run_early() {
		assertThat(new RateLimitingFilter(mock(RateLimiterRegistry.class)).getOrder()).isEqualTo(-100);
	}

}
