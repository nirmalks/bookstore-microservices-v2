package com.nirmalks.checkout_service.cart.dto;

public record CartItemDto(Long id, Long bookId, int quantity, double price) {
}