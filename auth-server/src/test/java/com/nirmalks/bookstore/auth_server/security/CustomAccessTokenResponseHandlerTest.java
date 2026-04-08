package com.nirmalks.bookstore.auth_server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomAccessTokenResponseHandlerTest {

	@InjectMocks
	private CustomAccessTokenResponseHandler handler;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private ServletOutputStream outputStream;

	@BeforeEach
	void setUp() throws IOException {
		lenient().when(response.getOutputStream()).thenReturn(outputStream);
	}

	@Test
	void onAuthenticationSuccess_should_write_token_response() throws IOException {
		// Given
		OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token-value",
				Instant.now(), Instant.now().plusSeconds(3600));

		Map<String, Object> additionalParameters = Map.of("userId", 123L, "username", "testuser");

		OAuth2AccessTokenAuthenticationToken authentication = mock(OAuth2AccessTokenAuthenticationToken.class);
		when(authentication.getAccessToken()).thenReturn(accessToken);
		when(authentication.getAdditionalParameters()).thenReturn(additionalParameters);
		when(authentication.getRefreshToken()).thenReturn(null);

		// When
		handler.onAuthenticationSuccess(request, response, authentication);

		// Then
		verify(response).setStatus(HttpServletResponse.SC_OK);
		verify(response).setContentType("application/json;charset=UTF-8");

		ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
		verify(outputStream).write(captor.capture(), anyInt(), anyInt());

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> body = mapper.readValue(captor.getValue(), Map.class);

		assertEquals("token-value", body.get("access_token"));
		assertEquals("Bearer", body.get("token_type"));
		assertEquals(3600, ((Number) body.get("expires_in")).intValue());
		assertEquals(123, body.get("userId"));
		assertEquals("testuser", body.get("username"));
	}

	@Test
	void onAuthenticationSuccess_should_set_error_status_when_not_access_token_authentication_token()
			throws IOException {
		org.springframework.security.core.Authentication authentication = mock(
				org.springframework.security.core.Authentication.class);

		handler.onAuthenticationSuccess(request, response, authentication);

		verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		verify(response, never()).getOutputStream();
	}

}
