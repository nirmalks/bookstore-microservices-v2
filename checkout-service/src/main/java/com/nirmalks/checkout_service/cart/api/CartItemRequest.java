package com.nirmalks.checkout_service.cart.api;

import jakarta.validation.constraints.NotNull;

public record CartItemRequest(@NotNull Long bookId, @NotNull int quantity) {
}
