package com.nirmalks.bookstore.api_gateway.security;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	public SecurityWebFilterChain mainSecurityFilterChain(ServerHttpSecurity http) {
		http.csrf(ServerHttpSecurity.CsrfSpec::disable)
			.authorizeExchange(exchanges -> exchanges.pathMatchers("/api/v1/users/register")
				.permitAll()
				.pathMatchers("/api/v1/users/admin/register")
				.permitAll()
				.pathMatchers("/api/v1/internal/**")
				.permitAll()
				.pathMatchers("/api/v1/login", "/api/v1/oauth2/token")
				.permitAll()
				.pathMatchers("/eureka/**")
				.permitAll()
				.pathMatchers("/actuator/**")
				.permitAll()
				.pathMatchers("/swagger-ui/**", "/v3/api-docs/**", "/error")
				.permitAll()
				.pathMatchers("/api/v1/books/**", "/api/v1/genres/**", "/api/v1/authors/**")
				.permitAll()
				.anyExchange()
				.authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return http.build();
	}

	@Bean
	public JwtHeaderPropagationFilter jwtHeaderPropagationFilter() {
		return new JwtHeaderPropagationFilter();
	}

	/**
	 * Defines routes programmatically for Spring Cloud Gateway. This method creates and
	 * configures the routing rules for various microservices.
	 * @param builder A builder for creating RouteLocator instances.
	 * @return A configured RouteLocator.
	 */
	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
			// --- User Service Login/Register Route ---
			.route("user-auth-route",
					r -> r.path("/api/v1/login", "/api/v1/register/**")
						.and()
						.method("GET", "POST", "PUT", "DELETE")
						.filters(f -> f.rewritePath("/api/v1/(?<segment>.*)", "/api/v1/${segment}")
							.circuitBreaker(c -> c.setName("userAuthCB").setFallbackUri("forward:/fallback/userauth")))
						.uri("lb://user-service"))

			// --- User Service General API Route ---
			.route("user-api-route",
					r -> r.path("/api/v1/users/**", "/api/v1/internal/**")
						.and()
						.method("GET", "POST", "PUT", "DELETE")
						.filters(f -> f.rewritePath("/api/v1/(?<segment>.*)", "/api/v1/${segment}")
							.circuitBreaker(c -> c.setName("userApiCB").setFallbackUri("forward:/fallback/user")))
						.uri("lb://user-service"))

			// --- Catalog Service Route ---
			.route("catalog-service-route",
					r -> r.path("/api/v1/books/**", "/api/v1/authors/**", "/api/v1/genres/**")
						.and()
						.method("GET", "POST", "PUT", "DELETE")
						.filters(f -> f.rewritePath("/api/v1/(?<segment>.*)", "/api/v1/${segment}")
							.circuitBreaker(c -> c.setName("catalogCB").setFallbackUri("forward:/fallback/catalog")))
						.uri("lb://catalog-service"))

			// --- Checkout Service Route ---
			.route("checkout-service-route",
					r -> r.path("/api/v1/cart/**", "/api/v1/orders/**")
						.and()
						.method("GET", "POST", "PUT", "DELETE")
						.filters(f -> f.rewritePath("/api/v1/(?<segment>.*)", "/api/v1/${segment}")
							.circuitBreaker(c -> c.setName("checkoutCB").setFallbackUri("forward:/fallback/checkout")))
						.uri("lb://checkout-service"))

			// auth server route
			.route("auth-server-route",
					r -> r.path("/api/v1/oauth2/token", "/api/v1/oauth/**")
						.and()
						.method("GET", "POST", "PUT", "DELETE")
						.filters(f -> f.rewritePath("/api/v1/(?<segment>.*)", "/api/v1/${segment}"))
						.uri("lb://auth-server"))
			.build();
	}

}
