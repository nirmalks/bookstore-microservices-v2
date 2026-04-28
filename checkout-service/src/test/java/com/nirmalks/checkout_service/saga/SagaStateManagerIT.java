package com.nirmalks.checkout_service.saga;

import com.nirmalks.checkout_service.AbstractIntegrationTest;
import com.nirmalks.checkout_service.config.RabbitMqConfig;
import com.nirmalks.checkout_service.order.entity.Order;
import com.nirmalks.checkout_service.order.entity.OrderStatus;
import com.nirmalks.checkout_service.order.repository.OrderRepository;
import dto.StockReservationFailedEvent;
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
 * Integration test for the End-to-End Saga flow. Tests how the system handles successful
 * coordination between services.
 */
class SagaStateManagerIT extends AbstractIntegrationTest {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private OrderRepository orderRepository;

	@Test
	@DisplayName("Should successfully complete full saga when all services acknowledge")
	void shouldCompleteFullSagaFlow() {
		// 1. Create a simulation order in the initial pending state
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
		StockReservationSuccessEvent successEvent = new StockReservationSuccessEvent(eventId, sagaId,
				orderId.toString(), List.of());

		// 2. Simulate StockReservationSuccessEvent from inventory
		// (InventoryResultConsumer will update order status and saga state)
		rabbitTemplate.convertAndSend(RabbitMqConfig.INVENTORY_EXCHANGE,
				RabbitMqConfig.STOCK_RESERVATION_SUCCESS_ROUTING_KEY, successEvent);

		// 3. Assert Saga state is COMPLETED and Order is CONFIRMED
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
			assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
			assertThat(updatedOrder.getSagaState()).isEqualTo(SagaState.SAGA_COMPLETED);
		});
	}

	@Test
	@DisplayName("Should execute compensation logic when stock reservation fails")
	void shouldCompensateWhenInventoryFails() {
		// 1. Create a simulation order
		String sagaId = UUID.randomUUID().toString();
		Order order = new Order();
		order.setUserId(2L);
		order.setTotalCost(50.0);
		order.setOrderStatus(OrderStatus.PENDING);
		order.setSagaId(sagaId);
		order.setSagaState(SagaState.STOCK_RESERVATION_PENDING);
		Order savedOrder = orderRepository.save(order);
		Long orderId = savedOrder.getId();

		String eventId = UUID.randomUUID().toString();
		StockReservationFailedEvent failedEvent = new StockReservationFailedEvent(eventId, sagaId, orderId.toString(),
				"Out of stock", List.of());

		// 2. Emit StockReservationFailedEvent
		rabbitTemplate.convertAndSend(RabbitMqConfig.INVENTORY_EXCHANGE,
				RabbitMqConfig.STOCK_RESERVATION_FAILED_ROUTING_KEY, failedEvent);

		// 3. Assert Saga state is COMPENSATED and Order is CANCELLED
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
			assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
			assertThat(updatedOrder.getSagaState()).isEqualTo(SagaState.SAGA_COMPENSATION_COMPLETED);
		});
	}

}
