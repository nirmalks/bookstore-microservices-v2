package com.nirmalks.checkout_service.cart.service;

import com.nirmalks.checkout_service.AbstractIntegrationTest;
import com.nirmalks.checkout_service.cart.api.CartItemRequest;
import com.nirmalks.checkout_service.cart.api.CartResponse;
import com.nirmalks.checkout_service.cart.repository.CartRepository;
import com.nirmalks.checkout_service.client.CatalogServiceClient;
import com.nirmalks.checkout_service.common.BookDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class CartServiceIT extends AbstractIntegrationTest {

	@Autowired
	private CartService cartService;

	@Autowired
	private CartRepository cartRepository;

	@MockitoBean
	private CatalogServiceClient catalogServiceClient;

	@BeforeEach
	void setUp() {
		cartRepository.deleteAll();
	}

	@Test
	void addToCart_should_add_item_to_cart_and_retrieve_it() {
		Long userId = 101L;
		Long bookId = 1L;
		CartItemRequest request = new CartItemRequest(bookId, 2);
		BookDto book = new BookDto(bookId, "Test Book", List.of(1L), 25.0, 10, "ISBN-123", LocalDate.now(), List.of(1L),
				"Description", "imagePath");

		when(catalogServiceClient.getBook(bookId)).thenReturn(book);

		CartResponse response = cartService.addToCart(userId, request);

		assertThat(response).isNotNull();
		assertThat(response.id()).isEqualTo(userId);
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).bookId()).isEqualTo(bookId);
		assertThat(response.items().get(0).quantity()).isEqualTo(2);

		// When: Add same item again
		cartService.addToCart(userId, new CartItemRequest(bookId, 3));

		CartResponse fetchedCart = cartService.getCart(userId);
		assertThat(fetchedCart.items()).hasSize(1);
		assertThat(fetchedCart.items().get(0).quantity()).isEqualTo(5); // 2 + 3
	}

	@Test
	void clearCart_should_clear_cart() {
		Long userId = 102L;
		CartItemRequest request = new CartItemRequest(1L, 1);
		BookDto book = new BookDto(1L, "Book", List.of(1L), 10.0, 10, "I", LocalDate.now(), List.of(1L), "D", "P");
		when(catalogServiceClient.getBook(1L)).thenReturn(book);
		cartService.addToCart(userId, request);

		cartService.clearCart(userId);

		CartResponse response = cartService.getCart(userId);
		assertThat(response.items()).isEmpty();
	}

}
