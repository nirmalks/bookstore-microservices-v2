package com.nirmalks.checkout_service.cart.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemRequest(@NotNull(message = "Book ID is required") Long bookId,
		@Min(value = 1, message = "Quantity must be at least 1") int quantity) {
}
