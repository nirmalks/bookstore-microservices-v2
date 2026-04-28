package com.nirmalks.checkout_service.order.api;

import com.nirmalks.checkout_service.order.dto.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DirectOrderRequest(@NotNull(message = "User ID is required") Long userId,
		@NotEmpty(message = "Order must contain at least one item") List<@Valid OrderItemRequest> items,
		@NotNull(message = "Shipping address is required") @Valid AddressRequest address) {
}