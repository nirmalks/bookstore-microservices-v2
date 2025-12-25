package com.nirmalks.checkout_service.actuator;

import com.nirmalks.checkout_service.order.entity.OrderStatus;
import com.nirmalks.checkout_service.order.repository.OrderRepository;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom actuator endpoint for order management insights Accessible at /actuator/orders
 */
@Component
@Endpoint(id = "orders")
public class OrderManagementEndpoint {

	private final OrderRepository orderRepository;

	public OrderManagementEndpoint(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	@ReadOperation
	public Map<String, Object> getOrderStats() {
		Map<String, Object> stats = new HashMap<>();

		long totalOrders = orderRepository.count();
		long pendingOrders = orderRepository.countByOrderStatus(OrderStatus.PENDING);
		long confirmedOrders = orderRepository.countByOrderStatus(OrderStatus.CONFIRMED);
		long cancelledOrders = orderRepository.countByOrderStatus(OrderStatus.CANCELLED);

		stats.put("total", totalOrders);
		stats.put("pending", pendingOrders);
		stats.put("confirmed", confirmedOrders);
		stats.put("cancelled", cancelledOrders);

		Map<String, Double> percentages = new HashMap<>();
		if (totalOrders > 0) {
			percentages.put("successRate", (confirmedOrders * 100.0) / totalOrders);
			percentages.put("cancellationRate", (cancelledOrders * 100.0) / totalOrders);
		}

		stats.put("percentages", percentages);
		return stats;
	}

	@ReadOperation
	public Map<String, Object> getOrderStatsByStatus(@Selector String status) {
		Map<String, Object> result = new HashMap<>();

		try {
			OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
			long count = orderRepository.countByOrderStatus(orderStatus);
			result.put("status", status);
			result.put("count", count);
		}
		catch (IllegalArgumentException e) {
			result.put("error", "Invalid status: " + status);
			result.put("validStatuses", OrderStatus.values());
		}

		return result;
	}

}
