package com.nirmalks.checkout_service.order.messaging;

import com.nirmalks.checkout_service.AbstractIntegrationTest;
import com.nirmalks.checkout_service.config.RabbitMqConfig;
import com.nirmalks.checkout_service.order.entity.Order;
import com.nirmalks.checkout_service.order.entity.OrderStatus;
import com.nirmalks.checkout_service.order.repository.OrderRepository;
import dto.StockReservationSuccessEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OrderEventMessagingIT extends AbstractIntegrationTest {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private OrderRepository orderRepository;

	@Test
	void stockReservedEventReceived_should_update_order_status_to_confirmed() {
		// Given: An order exists in PENDING status
		Order order = new Order();
		order.setUserId(200L);
		order.setOrderStatus(OrderStatus.PENDING);
		order.setPlacedDate(LocalDateTime.now());
		order.setTotalCost(100.0);
		Order savedOrder = orderRepository.save(order);

		StockReservationSuccessEvent event = new StockReservationSuccessEvent("event-" + savedOrder.getId(),
				"saga-" + savedOrder.getId(), savedOrder.getId().toString(),
				List.of(new StockReservationSuccessEvent.ReservedItem(1L, 2)));

		// When: A stock reservation success event is published to the inventory exchange
		// This simulates the catalog-service acknowledging the stock reservation
		rabbitTemplate.convertAndSend(RabbitMqConfig.INVENTORY_EXCHANGE,
				RabbitMqConfig.STOCK_RESERVATION_SUCCESS_ROUTING_KEY, event);

		// Then: The order status should eventually be updated to CONFIRMED by
		// InventoryResultConsumer
		await().atMost(10, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(500)).untilAsserted(() -> {
			Order updatedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
			assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
		});
	}

}
