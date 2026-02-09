package com.nirmalks.catalog_service.messaging;

import dto.StockReservationFailedEvent;
import dto.StockReservationSuccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryEventPublisher {

	private final Logger logger = LoggerFactory.getLogger(InventoryEventPublisher.class);

	private final RabbitTemplate rabbitTemplate;

	public InventoryEventPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	/**
	 * Publish stock reservation success event with saga context.
	 */
	public void publishStockReservedEvent(String sagaId, String orderId,
			List<StockReservationSuccessEvent.ReservedItem> reservedItems) {
		String eventId = UUID.randomUUID().toString();
		StockReservationSuccessEvent event = new StockReservationSuccessEvent(eventId, sagaId, orderId, reservedItems);

		logger.info("Publishing Stock Reserved event for saga: {}, orderId: {}, eventId: {}, items: {}", sagaId,
				orderId, eventId, reservedItems.size());

		rabbitTemplate.convertAndSend(RabbitMqConfig.INVENTORY_EXCHANGE,
				RabbitMqConfig.STOCK_RESERVATION_SUCCESS_ROUTING_KEY, event);
	}

	/**
	 * Publish stock reservation failed event with saga context.
	 */
	public void publishStockFailedEvent(String sagaId, String orderId, String reason,
			List<StockReservationFailedEvent.FailedItem> failedItems) {
		String eventId = UUID.randomUUID().toString();
		StockReservationFailedEvent event = new StockReservationFailedEvent(eventId, sagaId, orderId, reason,
				failedItems);

		logger.error(
				"Publishing Stock Failed event for saga: {}, orderId: {}, eventId: {}, reason: {}, failedItems: {}",
				sagaId, orderId, eventId, reason, failedItems.size());

		rabbitTemplate.convertAndSend(RabbitMqConfig.INVENTORY_EXCHANGE,
				RabbitMqConfig.STOCK_RESERVATION_FAILED_ROUTING_KEY, event);
	}

}