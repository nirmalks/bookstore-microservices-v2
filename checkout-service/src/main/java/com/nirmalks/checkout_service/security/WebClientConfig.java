package com.nirmalks.checkout_service.security;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Configuration class for creating and configuring WebClient instances. This class
 * provides beans for communicating with the user service and catalog service, and
 * includes a filter to propagate authentication headers for seamless service-to-service
 * communication on behalf of a user.
 *
 * Timeouts are configured to work with Resilience4j Circuit Breaker. - Connection
 * timeout: 2 seconds (fail fast if service is down) - Read timeout: 5 seconds (fail if
 * response takes too long)
 */
@Configuration
public class WebClientConfig {

	@Value("${user-service.base-url}")
	private String userServiceBaseUrl;

	@Value("${catalog-service.base-url}")
	private String catalogServiceBaseUrl;

	private static final int CONNECT_TIMEOUT_MS = 2000; // 2 seconds

	private static final int READ_TIMEOUT_SECONDS = 5; // 5 seconds

	private static final int WRITE_TIMEOUT_SECONDS = 5; // 5 seconds

	/**
	 * Creates a WebClient bean for communicating with the user service. Configured with
	 * timeouts and authentication header propagation.
	 * @return a WebClient instance for the user service.
	 */
	@Bean("userServiceWebClient")
	@LoadBalanced
	public WebClient userServiceWebClient(WebClient.Builder webClientBuilder) {
		return webClientBuilder.baseUrl(userServiceBaseUrl)
			.clientConnector(new ReactorClientHttpConnector(createHttpClient()))
			.filter(addAuthHeadersFilter())
			.build();
	}

	/**
	 * Creates a WebClient bean for communicating with the catalog service. Configured
	 * with timeouts and authentication header propagation.
	 * @return a WebClient instance for the catalog service.
	 */
	@Bean("catalogServiceWebClient")
	@LoadBalanced
	public WebClient catalogServiceWebClient(WebClient.Builder webClientBuilder) {
		return webClientBuilder.baseUrl(catalogServiceBaseUrl)
			.clientConnector(new ReactorClientHttpConnector(createHttpClient()))
			.filter(addAuthHeadersFilter())
			.build();
	}

	/**
	 * Creates an HttpClient with connection, read, and write timeouts. These timeouts
	 * ensure that calls fail fast when downstream services are unavailable, allowing the
	 * Circuit Breaker to open quickly.
	 */
	private HttpClient createHttpClient() {
		return HttpClient.create()
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
			.responseTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
			.doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS))
				.addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)));
	}

	/**
	 * An ExchangeFilterFunction to add user authentication headers to requests. It
	 * retrieves the user ID and roles from the SecurityContextHolder and adds them as
	 * headers to the outgoing WebClient request.
	 * @return an ExchangeFilterFunction for header propagation.
	 */
	private ExchangeFilterFunction addAuthHeadersFilter() {
		return (request, next) -> {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

			if (authentication != null && authentication.isAuthenticated()) {
				String userId = (String) authentication.getPrincipal();
				String roles = authentication.getAuthorities()
					.stream()
					.map(Object::toString)
					.collect(Collectors.joining(","));

				// Build a new request with the headers added
				ClientRequest newRequest = ClientRequest.from(request)
					.header("X-User-ID", userId)
					.header("X-User-Roles", roles)
					.build();

				// Proceed with the modified request
				return next.exchange(newRequest);
			}

			// If no user is authenticated, proceed with the original request
			return next.exchange(request);
		};
	}

}
