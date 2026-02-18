package com.nirmalks.checkout_service.cart.service;

import com.nirmalks.checkout_service.cart.api.CartItemRequest;
import com.nirmalks.checkout_service.cart.api.CartResponse;
import com.nirmalks.checkout_service.cart.dto.CartMapper;
import com.nirmalks.checkout_service.cart.entity.Cart;
import com.nirmalks.checkout_service.cart.entity.CartItem;
import com.nirmalks.checkout_service.cart.repository.CartItemRepository;
import com.nirmalks.checkout_service.cart.repository.CartRepository;
import com.nirmalks.checkout_service.client.CatalogServiceClient;
import com.nirmalks.checkout_service.common.BookDto;
import exceptions.ResourceNotFoundException;
import locking.DistributedLockService;
import locking.LockKeys;
import logging.Auditable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cart service implementation that handles shopping cart operations.
 */
@Service
public class CartServiceImpl implements CartService {

	private final CartRepository cartRepository;

	private final CatalogServiceClient catalogServiceClient;

	private final DistributedLockService distributedLockService;

	@Autowired
	public CartServiceImpl(CartRepository cartRepository, CatalogServiceClient catalogServiceClient,
			DistributedLockService distributedLockService) {
		this.cartRepository = cartRepository;
		this.catalogServiceClient = catalogServiceClient;
		this.distributedLockService = distributedLockService;
	}

	public CartResponse getCart(Long userId) {
		Cart cart = cartRepository.findByUserId(userId)
			.orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));
		return CartMapper.toResponse(cart);
	}

	@Override
	@Auditable(action = "ADD_TO_CART", resource = "CART", resourceId = "#result.id", detail = "add item to cart")
	public CartResponse addToCart(Long userId, CartItemRequest cartItemRequest) {
		String lockKey = LockKeys.userCart(userId);

		return distributedLockService.executeWithLock(lockKey, 3, 10, TimeUnit.SECONDS, () -> {
			BookDto book = catalogServiceClient.getBook(cartItemRequest.getBookId());
			Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> createCartForUser(userId));

			Cart updatedCart = cartRepository.save(CartMapper.toEntity(book, cart, cartItemRequest));
			return CartMapper.toResponse(updatedCart);
		});
	}

	@Override
	@Auditable(action = "CLEAR_CART", resource = "CART", resourceId = "#userId", detail = "clear cart")
	public void clearCart(Long userId) {
		String lockKey = LockKeys.userCart(userId);

		distributedLockService.executeWithLock(lockKey, 3, 10, TimeUnit.SECONDS, () -> {
			Cart cart = cartRepository.findByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
			cart.getCartItems().clear();
			cartRepository.save(cart);
		});
	}

	private Cart createCartForUser(Long userId) {
		Cart cart = new Cart();
		cart.setUserId(userId);
		return cartRepository.save(cart);
	}

	@Override
	@Auditable(action = "REMOVE_CART_ITEM", resource = "CART_ITEM", resourceId = "#itemId", detail = "remove cart item")
	public void removeItemFromCart(Long cartId, Long itemId) {
		Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

		List<CartItem> updatedItems = cart.getCartItems()
			.stream()
			.filter(item -> !item.getId().equals(itemId))
			.toList();

		cart.setCartItems(updatedItems);
		cart.setTotalPrice(updatedItems.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum());

		cartRepository.save(cart);
	}

}
