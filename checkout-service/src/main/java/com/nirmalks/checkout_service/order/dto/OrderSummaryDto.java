package com.nirmalks.checkout_service.order.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderSummaryDto(Long id, String status, LocalDateTime placedDate, Double totalCost,
		List<OrderItemDto> items, AddressDto address) {
}
