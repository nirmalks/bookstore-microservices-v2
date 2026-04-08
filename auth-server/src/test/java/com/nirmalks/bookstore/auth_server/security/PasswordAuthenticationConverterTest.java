package com.nirmalks.bookstore.auth_server.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
public class PasswordAuthenticationConverterTest {

	@InjectMocks
	PasswordAuthenticationConverter passwordAuthenticationConverter;

	@Mock
	RegisteredClientRepository registeredClientRepository;

	@Test
	void convert_should_return_null_if_grant_type_is_not_password() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameter("grant_type")).thenReturn("authorization_code");
		Authentication result = passwordAuthenticationConverter.convert(request);
		assertNull(result);
	}

	@Test
	void convert_should_return_null_if_grant_type_is_password_but_user_principal_is_not_present() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameter("grant_type")).thenReturn("password");
		when(request.getUserPrincipal()).thenReturn(null);
		Authentication result = passwordAuthenticationConverter.convert(request);
		assertNull(result);
	}

	@Test
	void convert_should_return_token_request_if_grant_type_is_password_and_user_principal_is_present() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getParameter("grant_type")).thenReturn("password");

		Authentication clientPrincipal = mock(Authentication.class);
		doReturn(List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))).when(clientPrincipal).getAuthorities();
		Object details = new Object();
		when(clientPrincipal.getDetails()).thenReturn(details);
		when(clientPrincipal.getName()).thenReturn("client-id");
		when(registeredClientRepository.findByClientId("client-id")).thenReturn(null);
		when(request.getUserPrincipal()).thenReturn(clientPrincipal);

		Authentication result = passwordAuthenticationConverter.convert(request);

		assertNotNull(result);
		assertEquals("client-id", result.getName());
		assertEquals(clientPrincipal, result.getPrincipal());
		assertEquals(List.of(new SimpleGrantedAuthority("ROLE_CLIENT")), result.getAuthorities());
		assertEquals(details, result.getDetails());
	}

}
