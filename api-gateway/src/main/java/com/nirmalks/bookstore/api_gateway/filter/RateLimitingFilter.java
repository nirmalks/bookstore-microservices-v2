package com.nirmalks.bookstore.api_gateway.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global Rate Limiting Filter for API Gateway.
 *
 * This filter applies rate limiting to ALL incoming requests using Resilience4j.
 * Different rate limiters are used based on the request path: - /api/v1/orders/** →
 * ordersRateLimiter (3 requests per 10 seconds) - /api/v1/books/** → catalogRateLimiter
 * (10 requests per 10 seconds) - All others → globalRateLimiter (5 requests per 10
 * seconds)
 *
 * When rate limit is exceeded, returns HTTP 429 Too Many Requests.
 */
@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

	private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

	private final RateLimiterRegistry rateLimiterRegistry;

	public RateLimitingFilter(RateLimiterRegistry rateLimiterRegistry) {
		this.rateLimiterRegistry = rateLimiterRegistry;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String path = exchange.getRequest().getPath().value();
		String rateLimiterName = selectRateLimiter(path);

		RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(rateLimiterName);

		// Try to acquire permission
		boolean permitted = rateLimiter.acquirePermission();

		if (!permitted) {
			logger.warn("Rate limit exceeded for path: {} using limiter: {}", path, rateLimiterName);
			exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
			exchange.getResponse().getHeaders().add("X-RateLimit-Limiter", rateLimiterName);
			exchange.getResponse()
				.getHeaders()
				.add("Retry-After",
						String.valueOf(rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod().toSeconds()));
			return exchange.getResponse().setComplete();
		}

		// Add rate limit headers to response
		exchange.getResponse()
			.getHeaders()
			.add("X-RateLimit-Remaining", String.valueOf(rateLimiter.getMetrics().getAvailablePermissions()));
		exchange.getResponse()
			.getHeaders()
			.add("X-RateLimit-Limit", String.valueOf(rateLimiter.getRateLimiterConfig().getLimitForPeriod()));

		return chain.filter(exchange);
	}

	/**
	 * Select the appropriate rate limiter based on the request path.
	 */
	private String selectRateLimiter(String path) {
		if (path.startsWith("/api/v1/orders")) {
			return "ordersRateLimiter";
		}
		else if (path.startsWith("/api/v1/books") || path.startsWith("/api/v1/catalog")) {
			return "catalogRateLimiter";
		}
		return "globalRateLimiter";
	}

	@Override
	public int getOrder() {
		// Run early in the filter chain
		return -100;
	}

}
