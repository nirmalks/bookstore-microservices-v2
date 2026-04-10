package com.nirmalks.user_service.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirmalks.user_service.auth.api.LoginRequest;
import com.nirmalks.user_service.auth.api.LoginResponse;
import com.nirmalks.user_service.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private AuthenticationManager authenticationManager;

	private LoginRequest loginRequest;

	private LoginResponse loginResponse;

	@BeforeEach
	void setUp() {
		loginRequest = new LoginRequest("testuser", "password");

		loginResponse = new LoginResponse();
		loginResponse.setToken("mocked-jwt-token");
		loginResponse.setUsername("testuser");
		loginResponse.setUserId(1L);
		loginResponse.setRole("CUSTOMER");
	}

	@Test
	void login_returns_jwt_and_user_details_when_authentication_succeeds() throws Exception {
		Authentication authentication = mock(Authentication.class);
		when(authentication.isAuthenticated()).thenReturn(true);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenReturn(authentication);
		when(userService.authenticate("testuser", "password")).thenReturn(loginResponse);

		mockMvc
			.perform(post("/api/v1/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").value("mocked-jwt-token"))
			.andExpect(jsonPath("$.username").value("testuser"))
			.andExpect(jsonPath("$.userId").value(1L))
			.andExpect(jsonPath("$.role").value("CUSTOMER"));

		verify(authenticationManager).authenticate(argThat(authenticationToken -> {
			if (!(authenticationToken instanceof UsernamePasswordAuthenticationToken token)) {
				return false;
			}
			return "testuser".equals(token.getPrincipal()) && "password".equals(token.getCredentials());
		}));
		verify(userService).authenticate("testuser", "password");
	}

	@Test
	void login_returns_401_unauthorized_when_credentials_are_incorrect() throws Exception {
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenThrow(new BadCredentialsException("Invalid credentials"));

		ServletException exception = assertThrows(ServletException.class,
				() -> mockMvc.perform(post("/api/v1/login").with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(loginRequest))));

		assertInstanceOf(BadCredentialsException.class, exception.getCause());
		assertEquals("Invalid credentials", exception.getCause().getMessage());
		verifyNoInteractions(userService);
	}

	@Test
	void login_returns_400_bad_request_when_login_request_is_invalid() throws Exception {
		LoginRequest invalidRequest = new LoginRequest("", "");

		mockMvc
			.perform(post("/api/v1/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andExpect(status().isBadRequest());

		verifyNoInteractions(authenticationManager, userService);
	}

}
