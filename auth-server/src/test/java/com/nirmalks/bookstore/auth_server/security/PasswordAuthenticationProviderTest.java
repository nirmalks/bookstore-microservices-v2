package com.nirmalks.bookstore.auth_server.security;

import com.nirmalks.bookstore.auth_server.dto.UserDtoInternal;
import dto.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import org.junit.jupiter.api.AfterEach;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordAuthenticationProviderTest {

	@Mock
	private OAuth2AuthorizationService authorizationService;

	@Mock
	private OAuth2TokenGenerator<?> tokenGenerator;

	@Mock
	private WebClient webClient;

	@InjectMocks
	private PasswordAuthenticationProvider provider;

	@Mock
	private WebClient.RequestBodyUriSpec requestBodyUriSpec;

	@Mock
	private WebClient.RequestBodySpec requestBodySpec;

	@Mock
	private WebClient.ResponseSpec responseSpec;

	@Mock
	private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

	@Mock
	private AuthorizationServerContext authorizationServerContext;

	@Mock
	private OAuth2PasswordAuthenticationToken authRequest;

	@Mock
	private RegisteredClient registeredClient;

	@Mock
	private Authentication clientPrincipal;

	@BeforeEach
	void setUp() {
		AuthorizationServerContextHolder.setContext(authorizationServerContext);
		// Common webClient mock setup
		lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
		lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
		lenient().when(requestBodySpec.attributes(any())).thenReturn(requestBodySpec);
		doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
		lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
	}

	@AfterEach
	void tearDown() {
		AuthorizationServerContextHolder.resetContext();
	}

	@Test
	void authenticate_with_valid_credentials_returns_authentication_token() {
		// Given
		setupAuthRequest();

		UserDtoInternal userDto = new UserDtoInternal();
		userDto.setId(1L);
		userDto.setUsername("testuser");
		userDto.setHashedPassword("hashed");
		userDto.setRole(UserRole.CUSTOMER);

		when(responseSpec.bodyToMono(UserDtoInternal.class)).thenReturn(Mono.just(userDto));

		OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
		when(accessToken.getTokenValue()).thenReturn("access-token");
		doReturn(accessToken).when(tokenGenerator).generate(any());

		when(registeredClient.getScopes()).thenReturn(Set.of("openid"));

		// When
		Authentication result = provider.authenticate(authRequest);

		// Then
		assertNotNull(result);
		assertTrue(result instanceof OAuth2AccessTokenAuthenticationToken);
		OAuth2AccessTokenAuthenticationToken token = (OAuth2AccessTokenAuthenticationToken) result;
		assertEquals("access-token", token.getAccessToken().getTokenValue());
		assertEquals("testuser", token.getAdditionalParameters().get("username"));
		assertEquals(1L, token.getAdditionalParameters().get("userId"));
		verify(authorizationService).save(any());
	}

	@Test
	void authenticate_WithInvalidUser_ThrowsBadCredentialsException() {
		// Given
		setupAuthRequest();

		when(responseSpec.bodyToMono(UserDtoInternal.class)).thenReturn(Mono.empty());

		// When & Then
		assertThrows(BadCredentialsException.class, () -> provider.authenticate(authRequest));
	}

	@Test
	void authenticate_WithIncompleteUserData_ThrowsBadCredentialsException() {
		// Given
		setupAuthRequest();

		UserDtoInternal userDto = new UserDtoInternal();
		userDto.setId(null); // Incomplete data

		when(responseSpec.bodyToMono(UserDtoInternal.class)).thenReturn(Mono.just(userDto));

		// When & Then
		assertThrows(BadCredentialsException.class, () -> provider.authenticate(authRequest));
	}

	@Test
	void supports_WithPasswordToken_ReturnsTrue() {
		assertTrue(provider.supports(OAuth2PasswordAuthenticationToken.class));
	}

	@Test
	void supports_WithOtherToken_ReturnsFalse() {
		assertFalse(provider.supports(Authentication.class));
	}

	private void setupAuthRequest() {
		when(authRequest.getAdditionalParameters()).thenReturn(Map.of("username", "testuser", "password", "password"));
		when(authRequest.getRegisteredClient()).thenReturn(registeredClient);
		when(authRequest.getClientPrincipal()).thenReturn(clientPrincipal);
	}

}
