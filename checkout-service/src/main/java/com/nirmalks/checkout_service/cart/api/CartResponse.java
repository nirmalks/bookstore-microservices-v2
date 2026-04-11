package com.nirmalks.checkout_service.cart.api;

import com.nirmalks.checkout_service.cart.dto.CartItemDto;

import java.util.List;

public record CartResponse(Long id, List<CartItemDto> items, double totalPrice) {
}