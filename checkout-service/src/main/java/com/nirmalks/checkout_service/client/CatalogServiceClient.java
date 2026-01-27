package com.nirmalks.checkout_service.client;

import com.nirmalks.checkout_service.common.BookDto;
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
 * Client for communicating with the Catalog Service. All Resilience4j patterns (Circuit
 * Breaker, Retry, Bulkhead, Rate Limiter) are applied here.
 */
@Service
public class CatalogServiceClient {

	private static final Logger logger = LoggerFactory.getLogger(CatalogServiceClient.class);

	private static final String CIRCUIT_BREAKER_NAME = "catalogService";

	private final WebClient catalogServiceWebClient;

	public CatalogServiceClient(@Qualifier("catalogServiceWebClient") WebClient catalogServiceWebClient) {
		this.catalogServiceWebClient = catalogServiceWebClient;
	}

	/**
	 * Fetches book details from the Catalog Service.
	 *
	 * Resilience4j aspect execution order (by default aspect ordering): 1. Retry
	 * (order=398, outermost) - Retries on failure, fallback after all attempts 2.
	 * CircuitBreaker (order=399) - Records failures, opens circuit if threshold met 3.
	 * RateLimiter (order=400) - Limits call rate 4. Bulkhead (order=401, innermost) -
	 * Limits concurrent calls
	 *
	 * IMPORTANT: fallbackMethod is on Retry (outermost), should not be used on other
	 * annotations incl circuitbreaker. This ensures: - Original exception propagates to
	 * Retry for retry attempts - CircuitBreaker just records failures (no fallback to
	 * consume exception) - Fallback only called after ALL retries are exhausted
	 */
	@Retry(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getBookFallback")
	@CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
	@RateLimiter(name = CIRCUIT_BREAKER_NAME)
	@Bulkhead(name = CIRCUIT_BREAKER_NAME)
	public BookDto getBook(Long bookId) {
		logger.info(">>> ATTEMPT: Fetching book with ID: {} from Catalog Service", bookId);

		try {
			BookDto book = catalogServiceWebClient.get()
				.uri("/api/v1/books/{id}", bookId)
				.retrieve()
				.bodyToMono(BookDto.class)
				.block();

			logger.info("<<< SUCCESS: Fetched book: {}", bookId);
			return book;
		}
		catch (WebClientResponseException ex) {
			logger.warn("<<< FAILED: WebClientResponseException for book ID: {} - Status: {}", bookId,
					ex.getStatusCode());
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				throw new ResourceNotFoundException("Book not found for ID: " + bookId);
			}
			throw ex;
		}
		catch (Exception ex) {
			logger.warn("<<< FAILED: {} for book ID: {} - Message: {}", ex.getClass().getSimpleName(), bookId,
					ex.getMessage());
			throw ex;
		}
	}

	/**
	 * Fallback method when Catalog Service is unavailable. Called by Circuit Breaker,
	 * Retry, and Rate Limiter when they trigger.
	 */
	@SuppressWarnings("unused") // Called via reflection by Resilience4j
	private BookDto getBookFallback(Long bookId, Throwable t) {
		logger.error("Catalog Service call failed for book ID: {}. Reason: {}", bookId, t.getMessage());

		if (t instanceof ResourceNotFoundException) {
			throw (ResourceNotFoundException) t;
		}

		throw new ServiceUnavailableException(
				"Catalog Service is currently unavailable. Cannot fetch book details. Please try again later.");
	}

}
