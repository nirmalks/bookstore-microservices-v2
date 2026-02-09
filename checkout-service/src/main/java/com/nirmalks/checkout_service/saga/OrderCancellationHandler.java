package com.nirmalks.checkout_service.saga;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.nirmalks.checkout_service.config.RabbitMqConfig;
import com.nirmalks.checkout_service.order.entity.Order;
import com.nirmalks.checkout_service.order.entity.OrderStatus;
import com.nirmalks.checkout_service.order.repository.OrderRepository;

import dto.StockReleaseEvent;
import jakarta.transaction.Transactional;
import saga.SagaState;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderCancellationHandler {

	private static final Logger logger = LoggerFactory.getLogger(OrderCancellationHandler.class);

	private final OrderRepository orderRepository;

	private final RabbitTemplate rabbitTemplate;

	private final SagaStateManager sagaStateManager;

	public OrderCancellationHandler(OrderRepository orderRepository, RabbitTemplate rabbitTemplate,
			SagaStateManager sagaStateManager) {
		this.orderRepository = orderRepository;
		this.rabbitTemplate = rabbitTemplate;
		this.sagaStateManager = sagaStateManager;
	}

	@Transactional
	public void cancelOrder(Long orderId, String reason) {
		Order order = orderRepository.findById(orderId)
			.orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

		if (order.getOrderStatus() == OrderStatus.CANCELLED) {
			logger.info("Order {} is already cancelled", orderId);
			return;
		}

		if (order.getOrderStatus() == OrderStatus.DELIVERED) {
			throw new IllegalStateException("Cannot cancel a delivered order");
		}

		String sagaId = order.getSagaId();

		sagaStateManager.updateSagaState(sagaId, SagaState.SAGA_COMPENSATING);
		// If stock was reserved, trigger release
		if (order.getSagaState() == SagaState.STOCK_RESERVED || order.getSagaState() == SagaState.SAGA_COMPLETED) {

			publishStockReleaseEvent(order, reason);
		}
		order.setOrderStatus(OrderStatus.CANCELLED);
		order.setCompensationReason(reason);
		orderRepository.save(order);

		sagaStateManager.completeSagaWithCompensation(sagaId, reason);

		logger.info("Order {} cancelled successfully. Reason: {}", orderId, reason);
	}

	private void publishStockReleaseEvent(Order order, String reason) {
		List<StockReleaseEvent.ReleaseItem> itemsToRelease = order.getItems()
			.stream()
			.map(item -> new StockReleaseEvent.ReleaseItem(item.getBookId(), item.getQuantity()))
			.toList();

		StockReleaseEvent event = new StockReleaseEvent(UUID.randomUUID().toString(), order.getSagaId(),
				order.getId().toString(), reason, itemsToRelease);

		rabbitTemplate.convertAndSend(RabbitMqConfig.INVENTORY_EXCHANGE, RabbitMqConfig.STOCK_RELEASE_ROUTING_KEY,
				event);

		logger.info("Published stock release event for saga: {}", order.getSagaId());
	}

}
