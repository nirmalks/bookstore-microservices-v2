package com.nirmalks.bookstore.auth_server.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.reactive.function.client.WebClient;

import com.nirmalks.bookstore.auth_server.dto.UserDtoInternal;

import dto.UserRole;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

	private CustomUserDetailsService customUserDetailsService;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private WebClient webClient;

	@BeforeEach
	void setUp() {
		customUserDetailsService = new CustomUserDetailsService(webClient, "http://user-service");
	}

	@Test
	void test_loadUserByUsername_will_throw_exception_if_user_not_found() {
		when(webClient.get().uri(anyString(), anyString()).retrieve().bodyToMono(UserDtoInternal.class))
			.thenReturn(Mono.empty());

		assertThrows(UsernameNotFoundException.class, () -> customUserDetailsService.loadUserByUsername("test"));
	}

	@Test
	void test_loadUserByUsername_should_return_user_details_if_user_is_found() {
		UserDtoInternal userDto = new UserDtoInternal();
		userDto.setId(1L);
		userDto.setUsername("testuser");
		userDto.setHashedPassword("password");
		userDto.setRole(UserRole.CUSTOMER);

		when(webClient.get().uri(anyString(), anyString()).retrieve().bodyToMono(UserDtoInternal.class))
			.thenReturn(Mono.just(userDto));

		UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

		assertNotNull(userDetails);
		assertEquals("testuser", userDetails.getUsername());
		assertEquals("password", userDetails.getPassword());
		assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER")));
	}

}
