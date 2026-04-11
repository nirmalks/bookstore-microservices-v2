package com.nirmalks.checkout_service.client;

import com.nirmalks.checkout_service.common.UserDto;
import exceptions.ResourceNotFoundException;
import exceptions.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceClientTest {

	@Mock
	private WebClient webClient;

	@Mock
	private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

	@Mock
	private WebClient.RequestHeadersSpec requestHeadersSpec;

	@Mock
	private WebClient.ResponseSpec responseSpec;

	@InjectMocks
	private UserServiceClient userServiceClient;

	private UserDto testUser;

	@BeforeEach
	void setUp() {
		testUser = new UserDto();
		testUser.setId(1L);
		testUser.setEmail("test@example.com");
		testUser.setUsername("johndoe");
	}

	@Test
	void getUser_returns_UserDto_when_user_is_found() {
		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(anyString(), anyLong())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(UserDto.class)).thenReturn(Mono.just(testUser));

		UserDto result = userServiceClient.getUser(1L);

		assertNotNull(result);
		assertEquals(testUser.getId(), result.getId());
		assertEquals(testUser.getEmail(), result.getEmail());
	}

	@Test
	void getUser_throws_ResourceNotFoundException_when_user_is_not_found() {
		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(anyString(), anyLong())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(UserDto.class)).thenReturn(Mono
			.error(WebClientResponseException.create(HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null)));

		assertThrows(ResourceNotFoundException.class, () -> userServiceClient.getUser(1L));
	}

	@Test
	void getUser_throws_WebClientResponseException_when_other_error_occurs() {
		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(anyString(), anyLong())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(UserDto.class)).thenReturn(Mono.error(WebClientResponseException
			.create(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error", null, null, null)));

		assertThrows(WebClientResponseException.class, () -> userServiceClient.getUser(1L));
	}

	@Test
	void getUserFallback_rethrows_ResourceNotFoundException_when_user_is_not_found() {
		ResourceNotFoundException ex = new ResourceNotFoundException("Not found");

		assertThrows(ResourceNotFoundException.class,
				() -> ReflectionTestUtils.invokeMethod(userServiceClient, "getUserFallback", 1L, ex));
	}

	@Test
	void getUserFallback_throws_ServiceUnavailableException_when_other_error_occurs() {
		RuntimeException ex = new RuntimeException("Something went wrong");

		assertThrows(ServiceUnavailableException.class,
				() -> ReflectionTestUtils.invokeMethod(userServiceClient, "getUserFallback", 1L, ex));
	}

}
