package com.nirmalks.checkout_service.order.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(@NotNull(message = "Book ID is required") Long bookId,
		@Min(value = 1, message = "Quantity must be at least 1") int quantity,
		@Positive(message = "Price must be positive") double price) {
}
