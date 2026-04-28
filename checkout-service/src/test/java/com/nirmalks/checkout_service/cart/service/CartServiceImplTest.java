package com.nirmalks.checkout_service.cart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nirmalks.checkout_service.cart.api.CartItemRequest;
import com.nirmalks.checkout_service.cart.api.CartResponse;
import com.nirmalks.checkout_service.cart.entity.Cart;
import com.nirmalks.checkout_service.cart.entity.CartItem;
import com.nirmalks.checkout_service.cart.repository.CartRepository;
import com.nirmalks.checkout_service.client.CatalogServiceClient;
import com.nirmalks.checkout_service.common.BookDto;

import exceptions.ResourceNotFoundException;
import locking.DistributedLockService;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

	@Mock
	private CartRepository cartRepository;

	@InjectMocks
	private CartServiceImpl cartService;

	@Mock
	private CatalogServiceClient catalogServiceClient;

	@Mock
	private DistributedLockService distributedLockService;

	@Mock
	private com.nirmalks.checkout_service.metrics.OrderMetrics orderMetrics;

	Cart cart;

	@BeforeEach
	void setup() {
		CartItem cartItem = new CartItem();
		cartItem.setId(1L);
		cartItem.setBookId(1L);
		cartItem.setQuantity(1);
		cartItem.setPrice(10.0);
		cart = new Cart();
		cart.setId(1L);
		cart.setUserId(1L);
		cart.setCartItems(new ArrayList<>(List.of(cartItem)));
	}

	@Test
	void getCart_returns_CartDto_when_cart_is_found() {
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
		CartResponse cartResponse = cartService.getCart(1L);
		assertNotNull(cartResponse);
		assertEquals(1L, cartResponse.id());
		assertEquals(1, cartResponse.items().size());
		assertEquals(1L, cartResponse.items().get(0).id());
		assertEquals(1L, cartResponse.items().get(0).bookId());
		assertEquals(1, cartResponse.items().get(0).quantity());
		assertEquals(10.0, cartResponse.items().get(0).price());
	}

	@Test
	void getCart_throws_ResourceNotFoundException_when_cart_is_not_found() {
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> cartService.getCart(1L));
	}

	@Test
	void addToCart_returns_CartDto_when_book_is_added() {
		CartItemRequest cartItemRequest = new CartItemRequest(1L, 1);
		doAnswer(invocation -> {
			Supplier<?> supplier = invocation.getArgument(4);
			return supplier.get();
		}).when(distributedLockService)
			.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Supplier.class));
		when(catalogServiceClient.getBook(1L)).thenReturn(new BookDto(1L, "Book 1", List.of(1L), 10.0, 10, "1234567890",
				LocalDate.now(), List.of(1L), "Description", "imagePath"));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
		when(cartRepository.save(any(Cart.class))).thenReturn(cart);
		CartResponse cartResponse = cartService.addToCart(1L, cartItemRequest);
		assertNotNull(cartResponse);
		assertEquals(1L, cartResponse.id());
		assertEquals(1, cartResponse.items().size());
		assertEquals(1L, cartResponse.items().get(0).id());
		assertEquals(1L, cartResponse.items().get(0).bookId());
		assertEquals(2, cartResponse.items().get(0).quantity());
		assertEquals(10.0, cartResponse.items().get(0).price());
		verify(orderMetrics).incrementCartItemsAdded();
	}

	@Test
	void addToCart_throws_ResourceNotFoundException_when_book_is_not_found() {
		CartItemRequest cartItemRequest = new CartItemRequest(1L, 1);
		when(catalogServiceClient.getBook(1L)).thenThrow(ResourceNotFoundException.class);
		doAnswer(invocation -> {
			Supplier<?> supplier = invocation.getArgument(4);
			return supplier.get();
		}).when(distributedLockService)
			.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Supplier.class));
		assertThrows(ResourceNotFoundException.class, () -> cartService.addToCart(1L, cartItemRequest));
		verify(orderMetrics).incrementCartItemsAdded();
	}

	@Test
	void removeFromCart_will_remove_book_from_cart() {
		when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
		when(cartRepository.save(any(Cart.class))).thenReturn(cart);
		cartService.removeItemFromCart(1L, 1L);
		verify(cartRepository).save(any(Cart.class));
	}

	@Test
	void removeFromCart_throws_ResourceNotFoundException_when_cart_is_not_found() {
		when(cartRepository.findById(1L)).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> cartService.removeItemFromCart(1L, 1L));
	}

	@Test
	void clearCart_will_clear_cart_if_cart_is_found() {
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
		when(cartRepository.save(any(Cart.class))).thenReturn(cart);
		doAnswer(invocation -> {
			Runnable runnable = invocation.getArgument(4);
			runnable.run();
			return null;
		}).when(distributedLockService)
			.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));
		cartService.clearCart(1L);
		verify(cartRepository).save(any(Cart.class));
	}

	@Test
	void clearCart_throws_ResourceNotFoundException_when_cart_is_not_found() {
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
		doAnswer(invocation -> {
			Runnable runnable = invocation.getArgument(4);
			runnable.run();
			return null;
		}).when(distributedLockService)
			.executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));
		assertThrows(ResourceNotFoundException.class, () -> cartService.clearCart(1L));
	}

}
