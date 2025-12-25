package com.nirmalks.checkout_service.actuator;

import com.nirmalks.checkout_service.order.repository.OrderRepository;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom info contributor for actuator /info endpoint Shows service statistics and
 * runtime information
 */
@Component
public class CustomInfoContributor implements InfoContributor {

	private final OrderRepository orderRepository;

	public CustomInfoContributor(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	@Override
	public void contribute(Info.Builder builder) {
		Map<String, Object> serviceInfo = new HashMap<>();
		serviceInfo.put("name", "Checkout Service");
		serviceInfo.put("description", "Handles order creation and checkout operations");
		serviceInfo.put("version", "1.0.0");
		serviceInfo.put("team", "BE Engineering");

		Map<String, Object> statistics = new HashMap<>();
		statistics.put("totalOrders", orderRepository.count());
		statistics.put("lastUpdated", LocalDateTime.now().toString());

		Map<String, Object> features = new HashMap<>();
		features.put("asyncProcessing", true);
		features.put("eventDriven", true);
		features.put("circuitBreaker", true);
		features.put("rateLimiting", true);
		features.put("bulkhead", true);
		features.put("distributedTracing", true);

		builder.withDetail("service", serviceInfo)
			.withDetail("statistics", statistics)
			.withDetail("features", features);
	}

}
