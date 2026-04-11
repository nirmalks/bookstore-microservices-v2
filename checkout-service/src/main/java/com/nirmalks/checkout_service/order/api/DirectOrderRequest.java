package com.nirmalks.checkout_service.order.api;

import com.nirmalks.checkout_service.order.dto.AddressRequest;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DirectOrderRequest(@NotNull Long userId, @NotNull List<OrderItemRequest> items,
		@NotNull AddressRequest address) {
}