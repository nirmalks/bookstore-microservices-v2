package com.nirmalks.catalog_service.messaging;

import dto.StockReservationFailedEvent;
import dto.StockReservationSuccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventoryEventPublisher {

	private final Logger logger = LoggerFactory.getLogger(InventoryEventPublisher.class);

	private final RabbitTemplate rabbitTemplate;

	public InventoryEventPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public void publishStockReservedEvent(String orderId) {
		StockReservationSuccessEvent event = new StockReservationSuccessEvent(orderId);
		logger.info("Publishing Stock Reserved event for Order ID: {}", orderId);

		rabbitTemplate.convertAndSend(RabbitMqConfig.INVENTORY_EXCHANGE,
				RabbitMqConfig.STOCK_RESERVATION_SUCCESS_ROUTING_KEY, event);
	}

	public void publishStockFailedEvent(String orderId, String reason) {
		StockReservationFailedEvent event = new StockReservationFailedEvent(orderId, reason);
		logger.error("Publishing Stock Failed event for Order ID: {} with reason: {}", orderId, reason);

		rabbitTemplate.convertAndSend(RabbitMqConfig.INVENTORY_EXCHANGE,
				RabbitMqConfig.STOCK_RESERVATION_FAILED_ROUTING_KEY, event);
	}

}