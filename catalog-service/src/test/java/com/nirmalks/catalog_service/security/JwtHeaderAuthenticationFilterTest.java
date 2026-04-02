package com.nirmalks.catalog_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class JwtHeaderAuthenticationFilterTest {

	@InjectMocks
	private JwtHeaderAuthenticationFilter jwtHeaderAuthenticationFilter;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void doFilterInternal_should_set_authentication_when_headers_are_present() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-User-ID", "user123");
		request.addHeader("X-User-Roles", "ROLE_USER,ROLE_ADMIN");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		jwtHeaderAuthenticationFilter.doFilterInternal(request, response, filterChain);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertNotNull(authentication);
		assertEquals("user123", authentication.getPrincipal());

		Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
		assertEquals(2, authorities.size());
		assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
		assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilterInternal_should_not_set_authentication_when_headers_are_missing()
			throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		jwtHeaderAuthenticationFilter.doFilterInternal(request, response, filterChain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilterInternal_should_not_set_authentication_when_only_user_id_is_present()
			throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-User-ID", "user123");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		jwtHeaderAuthenticationFilter.doFilterInternal(request, response, filterChain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void doFilterInternal_should_not_set_authentication_when_only_roles_are_present()
			throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-User-Roles", "ROLE_USER");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = mock(FilterChain.class);

		jwtHeaderAuthenticationFilter.doFilterInternal(request, response, filterChain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void shouldNotFilter_should_return_true_for_excluded_endpoints() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		request.setRequestURI("/api/v1/internal/health");
		assertTrue(jwtHeaderAuthenticationFilter.shouldNotFilter(request));

		request.setRequestURI("/v3/api-docs");
		assertTrue(jwtHeaderAuthenticationFilter.shouldNotFilter(request));

		request.setRequestURI("/api/v1/books");
		assertFalse(jwtHeaderAuthenticationFilter.shouldNotFilter(request));
	}

}
