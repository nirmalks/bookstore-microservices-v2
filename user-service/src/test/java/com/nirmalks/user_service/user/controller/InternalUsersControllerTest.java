package com.nirmalks.user_service.user.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirmalks.user_service.auth.api.LoginRequest;
import com.nirmalks.user_service.user.dto.UserDtoInternal;
import com.nirmalks.user_service.user.service.UserService;

import dto.UserRole;
import jakarta.servlet.ServletException;

@WebMvcTest(InternalUsersController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalUsersControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	private AuthenticationManager authenticationManager;

	@MockitoBean
	private UserService userService;

	@Test
	void internalAuth_should_return_bad_credentials_exception_when_invalid_credentials() throws Exception {
		Authentication authentication = mock(Authentication.class);
		LoginRequest loginRequest = new LoginRequest("admin", "admin");
		when(authentication.isAuthenticated()).thenReturn(false);
		when(authenticationManager.authenticate(any())).thenReturn(authentication);
		ServletException exception = assertThrows(ServletException.class,
				() -> mockMvc.perform(post("/api/v1/internal/users/auth").contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(loginRequest))));
		assertInstanceOf(BadCredentialsException.class, exception.getCause());
		assertEquals("Invalid credentials", exception.getCause().getMessage());
	}

	@Test
	void internalAuth_should_return_valid_dto_if_credentials_are_valid() throws Exception {
		Authentication authentication = mock(Authentication.class);
		LoginRequest loginRequest = new LoginRequest("admin", "admin");
		when(authentication.isAuthenticated()).thenReturn(true);
		when(authenticationManager.authenticate(any())).thenReturn(authentication);
		when(userService.internalAuthenticate(any(), any()))
			.thenReturn(new UserDtoInternal(1L, "admin", "admin", UserRole.ADMIN));
		mockMvc
			.perform(post("/api/v1/internal/users/auth").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("admin"))
			.andExpect(jsonPath("$.hashedPassword").value("admin"))
			.andExpect(jsonPath("$.role").value("ADMIN"))
			.andExpect(jsonPath("$.id").value(1L));
	}

}
