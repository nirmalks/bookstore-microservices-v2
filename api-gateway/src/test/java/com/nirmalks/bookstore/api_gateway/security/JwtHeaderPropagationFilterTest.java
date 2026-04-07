package com.nirmalks.bookstore.api_gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class JwtHeaderPropagationFilterTest {

	private final JwtHeaderPropagationFilter filter = new JwtHeaderPropagationFilter();

	@Test
	void filter_should_propagate_user_id_and_roles_from_jwt_claims() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/books").build());
		AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();

		Jwt jwt = new Jwt("token", Instant.parse("2026-04-02T10:00:00Z"), Instant.parse("2026-04-02T11:00:00Z"),
				Map.of("alg", "none"), Map.of("sub", "user-42", "roles", "[ROLE_USER, ROLE_ADMIN]"));

		Mono<Void> result = withAuthentication(filter.filter(exchange, chainCapturing(forwardedRequest)), jwt,
				List.of(new SimpleGrantedAuthority("ROLE_IGNORED")));

		StepVerifier.create(result).verifyComplete();

		assertThat(forwardedRequest.get()).isNotNull();
		assertThat(forwardedRequest.get().getHeaders().getFirst("X-User-ID")).isEqualTo("user-42");
		assertThat(forwardedRequest.get().getHeaders().getFirst("X-User-Roles")).isEqualTo("ROLE_USER, ROLE_ADMIN");
	}

	@Test
	void filter_should_fall_back_to_authorities_when_roles_claim_is_missing() {
		MockServerWebExchange exchange = MockServerWebExchange
			.from(MockServerHttpRequest.get("/api/v1/orders").build());
		AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();

		Jwt jwt = new Jwt("token", Instant.parse("2026-04-02T10:00:00Z"), Instant.parse("2026-04-02T11:00:00Z"),
				Map.of("alg", "none"), Map.of("sub", "user-99"));

		Mono<Void> result = withAuthentication(filter.filter(exchange, chainCapturing(forwardedRequest)), jwt,
				List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));

		StepVerifier.create(result).verifyComplete();

		assertThat(forwardedRequest.get().getHeaders().getFirst("X-User-ID")).isEqualTo("user-99");
		assertThat(forwardedRequest.get().getHeaders().getFirst("X-User-Roles")).isEqualTo("ROLE_USER,ROLE_ADMIN");
	}

	@Test
	void filter_should_leave_request_unchanged_when_authentication_is_missing() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/books").build());
		AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();

		StepVerifier.create(filter.filter(exchange, chainCapturing(forwardedRequest))).verifyComplete();

		assertThat(forwardedRequest.get()).isNotNull();
		assertThat(forwardedRequest.get().getHeaders().containsKey("X-User-ID")).isFalse();
		assertThat(forwardedRequest.get().getHeaders().containsKey("X-User-Roles")).isFalse();
	}

	@Test
	void filter_get_order_should_match_lowest_precedence() {
		assertThat(filter.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
	}

	private GatewayFilterChain chainCapturing(AtomicReference<ServerHttpRequest> forwardedRequest) {
		return exchange -> {
			forwardedRequest.set(exchange.getRequest());
			return Mono.empty();
		};
	}

	private Mono<Void> withAuthentication(Mono<Void> mono, Jwt jwt, List<SimpleGrantedAuthority> authorities) {
		TestingAuthenticationToken authentication = new TestingAuthenticationToken(jwt, null, authorities);
		authentication.setAuthenticated(true);
		return mono.contextWrite(org.springframework.security.core.context.ReactiveSecurityContextHolder
			.withSecurityContext(Mono.just(new SecurityContextImpl(authentication))));
	}

}
