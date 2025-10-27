package com.nirmalks.checkout_service.order.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record OrderMessage(String orderId,
                           Long userId,
                           String email,
                           double totalCost,
                           LocalDateTime placedAt,
                           List<String> bookTitles) implements Serializable {
}
