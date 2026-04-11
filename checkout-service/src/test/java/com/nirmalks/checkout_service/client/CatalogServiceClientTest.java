package com.nirmalks.checkout_service.client;

import com.nirmalks.checkout_service.common.BookDto;
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
class CatalogServiceClientTest {

	@Mock
	private WebClient webClient;

	@Mock
	private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

	@Mock
	private WebClient.RequestHeadersSpec requestHeadersSpec;

	@Mock
	private WebClient.ResponseSpec responseSpec;

	@InjectMocks
	private CatalogServiceClient catalogServiceClient;

	private BookDto testBook;

	@BeforeEach
	void setUp() {
		testBook = new BookDto();
		testBook.setId(1L);
		testBook.setTitle("Test Book");
		testBook.setPrice(19.99);
		testBook.setStock(10);
	}

	@Test
	void getBook_returns_BookDto_when_book_is_found() {
		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(anyString(), anyLong())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(BookDto.class)).thenReturn(Mono.just(testBook));

		BookDto result = catalogServiceClient.getBook(1L);

		assertNotNull(result);
		assertEquals(testBook.getId(), result.getId());
		assertEquals(testBook.getTitle(), result.getTitle());
	}

	@Test
	void getBook_throws_ResourceNotFoundException_when_book_is_not_found() {
		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(anyString(), anyLong())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(BookDto.class)).thenReturn(Mono
			.error(WebClientResponseException.create(HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null)));

		assertThrows(ResourceNotFoundException.class, () -> catalogServiceClient.getBook(1L));
	}

	@Test
	void getBook_throws_WebClientResponseException_when_other_error_occurs() {
		when(webClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(anyString(), anyLong())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(BookDto.class)).thenReturn(Mono.error(WebClientResponseException
			.create(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error", null, null, null)));

		assertThrows(WebClientResponseException.class, () -> catalogServiceClient.getBook(1L));
	}

	@Test
	void getBookFallback_rethrows_ResourceNotFoundException_when_book_is_not_found() {
		ResourceNotFoundException ex = new ResourceNotFoundException("Not found");

		assertThrows(ResourceNotFoundException.class,
				() -> ReflectionTestUtils.invokeMethod(catalogServiceClient, "getBookFallback", 1L, ex));
	}

	@Test
	void getBookFallback_throws_ServiceUnavailableException_when_other_error_occurs() {
		RuntimeException ex = new RuntimeException("Something went wrong");

		assertThrows(ServiceUnavailableException.class,
				() -> ReflectionTestUtils.invokeMethod(catalogServiceClient, "getBookFallback", 1L, ex));
	}

}
