package com.nirmalks.user_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class InternalApiJwtConverterTest {

	private final InternalApiJwtConverter converter = new InternalApiJwtConverter();

	@BeforeEach
	void setUp() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void convert_should_return_token_with_authorities_from_scopes() {
		Jwt jwt = mock(Jwt.class);
		when(jwt.getClaimAsStringList("scope")).thenReturn(List.of("read", "write"));
		when(jwt.getClaimAsString("client_id")).thenReturn("other-client");

		MockHttpServletRequest request = (MockHttpServletRequest) ((ServletRequestAttributes) RequestContextHolder
			.getRequestAttributes()).getRequest();
		request.setRequestURI("/api/v1/users");

		AbstractAuthenticationToken token = converter.convert(jwt);

		assertNotNull(token);
		assertEquals(2, token.getAuthorities().size());
		assertTrue(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("SCOPE_read")));
		assertTrue(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("SCOPE_write")));
	}

	@Test
	void convert_should_throw_exception_for_internal_endpoint_if_client_id_is_not_auth_server() {
		Jwt jwt = mock(Jwt.class);
		when(jwt.getClaimAsString("client_id")).thenReturn("unauthorized-client");

		MockHttpServletRequest request = (MockHttpServletRequest) ((ServletRequestAttributes) RequestContextHolder
			.getRequestAttributes()).getRequest();
		request.setRequestURI("/api/v1/internal/users/auth");

		RuntimeException exception = assertThrows(RuntimeException.class, () -> converter.convert(jwt));
		assertEquals("Not authorized for internal endpoint", exception.getMessage());
	}

	@Test
	void convert_should_succeed_for_internal_endpoint_if_client_id_is_auth_server() {
		Jwt jwt = mock(Jwt.class);
		when(jwt.getClaimAsString("client_id")).thenReturn("auth-server-client");
		when(jwt.getClaimAsStringList("scope")).thenReturn(List.of("internal_api"));

		MockHttpServletRequest request = (MockHttpServletRequest) ((ServletRequestAttributes) RequestContextHolder
			.getRequestAttributes()).getRequest();
		request.setRequestURI("/api/v1/internal/users/auth");

		AbstractAuthenticationToken token = converter.convert(jwt);

		assertNotNull(token);
		assertTrue(token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("SCOPE_internal_api")));
	}

	private boolean assertTrue(boolean condition) {
		if (!condition) {
			throw new AssertionError("Condition expected to be true");
		}
		return true;
	}

}
