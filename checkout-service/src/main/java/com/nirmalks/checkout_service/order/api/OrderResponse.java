package com.nirmalks.checkout_service.order.api;

import com.nirmalks.checkout_service.common.UserDto;
import com.nirmalks.checkout_service.order.dto.OrderSummaryDto;

public record OrderResponse(OrderSummaryDto order, UserDto user, String message) {
}
