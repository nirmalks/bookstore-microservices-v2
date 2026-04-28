package com.nirmalks.checkout_service.idempotency;

import com.nirmalks.checkout_service.AbstractIntegrationTest;
import com.nirmalks.checkout_service.config.RabbitMqConfig;
import com.nirmalks.checkout_service.idempotency.repository.ProcessedEventRepository;
import com.nirmalks.checkout_service.order.entity.Order;
import com.nirmalks.checkout_service.order.entity.OrderStatus;
import com.nirmalks.checkout_service.order.repository.OrderRepository;
import dto.StockReservationSuccessEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import saga.SagaState;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for Idempotent Event Processing. Ensures the system handles duplicate
 * messages without side effects.
 */
class IdempotentConsumerIT extends AbstractIntegrationTest {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private ProcessedEventRepository processedEventRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Test
	@DisplayName("Should process an event only once even if message is duplicated")
	void sending_duplicate_events_should_be_processed_only_once() {
		// 1. Create and save a dummy order to be processed
		String sagaId = UUID.randomUUID().toString();
		Order order = new Order();
		order.setUserId(1L);
		order.setTotalCost(100.0);
		order.setOrderStatus(OrderStatus.PENDING);
		order.setSagaId(sagaId);
		order.setSagaState(SagaState.STOCK_RESERVATION_PENDING);
		Order savedOrder = orderRepository.save(order);
		Long orderId = savedOrder.getId();

		String eventId = UUID.randomUUID().toString();
		StockReservationSuccessEvent event = new StockReservationSuccessEvent(eventId, sagaId, orderId.toString(),
				List.of());

		// 2. Send the EXACT SAME event twice immediately
		// sending to the exchange that InventoryResultConsumer listens to
		rabbitTemplate.convertAndSend(RabbitMqConfig.INVENTORY_EXCHANGE,
				RabbitMqConfig.STOCK_RESERVATION_SUCCESS_ROUTING_KEY, event);
		rabbitTemplate.convertAndSend(RabbitMqConfig.INVENTORY_EXCHANGE,
				RabbitMqConfig.STOCK_RESERVATION_SUCCESS_ROUTING_KEY, event);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			boolean processed = processedEventRepository.existsByEventId(eventId);
			assertThat(processed).as("Event should be marked as processed").isTrue();

			Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
			assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
			assertThat(updatedOrder.getSagaState()).isEqualTo(SagaState.SAGA_COMPLETED);
		});

		assertThat(processedEventRepository.existsByEventId(eventId)).isTrue();
	}

}
