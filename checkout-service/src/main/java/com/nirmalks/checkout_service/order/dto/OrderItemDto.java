package com.nirmalks.checkout_service.order.dto;

public record OrderItemDto(Long id, Long orderId, Long bookId, int quantity, double price, String name) {
}
