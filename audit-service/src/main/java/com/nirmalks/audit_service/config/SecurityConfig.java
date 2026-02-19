package com.nirmalks.audit_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/**")
				.permitAll() // Allow health checks
				.requestMatchers("/api/v1/audit/**")
				.hasAnyAuthority("ROLE_ADMIN") // Restrict audit API
				.anyRequest()
				.authenticated())
			.oauth2ResourceServer(
					oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

		return http.build();
	}

	@Bean
	public org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthenticationConverter() {
		org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter();
		grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
		grantedAuthoritiesConverter.setAuthorityPrefix("");

		org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthenticationConverter = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
		return jwtAuthenticationConverter;
	}

}
