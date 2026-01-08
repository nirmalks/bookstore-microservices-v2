package com.nirmalks.catalog_service.messaging;

import dto.StockReservationFailedEvent;
import dto.StockReservationSuccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InventoryEventPublisher {

	private final Logger logger = LoggerFactory.getLogger(InventoryEventPublisher.class);

	private final RabbitTemplate rabbitTemplate;

	public InventoryEventPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public void publishStockReservedEvent(String orderId) {
		String eventId = UUID.randomUUID().toString();
		StockReservationSuccessEvent event = new StockReservationSuccessEvent(eventId, orderId);
		logger.info("Publishing Stock Reserved event for Order ID: {}, eventId: {}", orderId, eventId);

		rabbitTemplate.convertAndSend(RabbitMqConfig.INVENTORY_EXCHANGE,
				RabbitMqConfig.STOCK_RESERVATION_SUCCESS_ROUTING_KEY, event);
	}

	public void publishStockFailedEvent(String orderId, String reason) {
		String eventId = UUID.randomUUID().toString();
		StockReservationFailedEvent event = new StockReservationFailedEvent(eventId, orderId, reason);
		logger.error("Publishing Stock Failed event for Order ID: {}, eventId: {}, reason: {}", orderId, eventId,
				reason);

		rabbitTemplate.convertAndSend(RabbitMqConfig.INVENTORY_EXCHANGE,
				RabbitMqConfig.STOCK_RESERVATION_FAILED_ROUTING_KEY, event);
	}

}