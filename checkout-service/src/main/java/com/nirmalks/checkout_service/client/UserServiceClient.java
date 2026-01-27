package com.nirmalks.checkout_service.client;

import com.nirmalks.checkout_service.common.UserDto;
import exceptions.ResourceNotFoundException;
import exceptions.ServiceUnavailableException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Client for communicating with the User Service. All Resilience4j patterns (Circuit
 * Breaker, Retry, Bulkhead, Rate Limiter) are applied here.
 *
 * By extracting these calls into a separate service class, we ensure that: 1. Spring AOP
 * proxies are properly applied (no self-invocation issue) 2. All resilience patterns work
 * correctly 3. Centralized error handling for user service calls
 */
@Service
public class UserServiceClient {

	private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

	private static final String CIRCUIT_BREAKER_NAME = "userService";

	private final WebClient userServiceWebClient;

	public UserServiceClient(@Qualifier("userServiceWebClient") WebClient userServiceWebClient) {
		this.userServiceWebClient = userServiceWebClient;
	}

	/**
	 * Fetches user details from the User Service.
	 *
	 * Resilience4j aspect execution order (by default aspect ordering): 1. Retry
	 * (order=398, outermost) - Retries on failure, fallback after all attempts 2.
	 * CircuitBreaker (order=399) - Records failures, opens circuit if threshold met 3.
	 * RateLimiter (order=400) - Limits call rate 4. Bulkhead (order=401, innermost) -
	 * Limits concurrent calls
	 *
	 * IMPORTANT: fallbackMethod is on Retry (outermost), NOT CircuitBreaker! This
	 * ensures: - Original exception propagates to Retry for retry attempts -
	 * CircuitBreaker just records failures (no fallback to consume exception) - Fallback
	 * only called after ALL retries are exhausted
	 */
	@Retry(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserFallback")
	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
	@RateLimiter(name = CIRCUIT_BREAKER_NAME)
	@Bulkhead(name = CIRCUIT_BREAKER_NAME)
	public UserDto getUser(Long userId) {
		logger.debug("Fetching user with ID: {} from User Service", userId);

		try {
			UserDto user = userServiceWebClient.get()
				.uri("/api/v1/users/{id}", userId)
				.retrieve()
				.bodyToMono(UserDto.class)
				.block();

			logger.debug("Successfully fetched user: {}", userId);
			return user;
		}
		catch (WebClientResponseException ex) {
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				throw new ResourceNotFoundException("User not found for ID: " + userId);
			}
			// Let other exceptions propagate to trigger Circuit Breaker
			throw ex;
		}
	}

	/**
	 * Fallback method when User Service is unavailable. Called by Circuit Breaker, Retry,
	 * and Rate Limiter when they trigger.
	 */
	@SuppressWarnings("unused") // Called via reflection by Resilience4j
	private UserDto getUserFallback(Long userId, Throwable t) {
		logger.error("User Service call failed for user ID: {}. Reason: {}", userId, t.getMessage());

		if (t instanceof ResourceNotFoundException) {
			// Don't wrap ResourceNotFoundException - it's a business exception
			throw (ResourceNotFoundException) t;
		}

		throw new ServiceUnavailableException(
				"User Service is currently unavailable. Cannot fetch user details. Please try again later.");
	}

}
