package com.nirmalks.catalog_service.messaging;

import com.nirmalks.catalog_service.book.service.BookService;
import com.nirmalks.catalog_service.idempotency.entity.ProcessedEvent;
import com.nirmalks.catalog_service.idempotency.repository.ProcessedEventRepository;
import com.rabbitmq.client.Channel;
import dto.OrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class OrderCreatedEventConsumer {

	private static final String EVENT_TYPE = "ORDER_CREATED";

	private final Logger logger = LoggerFactory.getLogger(OrderCreatedEventConsumer.class);

	private final BookService bookService;

	private final InventoryEventPublisher eventPublisher;

	private final ProcessedEventRepository processedEventRepository;

	OrderCreatedEventConsumer(BookService bookService, InventoryEventPublisher eventPublisher,
			ProcessedEventRepository processedEventRepository) {
		this.bookService = bookService;
		this.eventPublisher = eventPublisher;
		this.processedEventRepository = processedEventRepository;
	}

	@RabbitListener(queues = RabbitMqConfig.QUEUE_INVENTORY)
	@Transactional
	public void consumeOrderCreatedEvent(OrderMessage message, Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
		String eventId = message.eventId();

		// skip processing if event is already processed to ensure consumer idempotency
		if (eventId != null && processedEventRepository.existsByEventId(eventId)) {
			logger.info("Duplicate event detected, skipping. eventId={}, orderId={}", eventId, message.orderId());
			return;
		}

		logger.info("Processing order event: eventId={}, orderId={}", eventId, message.orderId());

		try {
			bookService.updateStock(message);
			eventPublisher.publishStockReservedEvent(message.orderId());

			// mark event as processed to ensure consumer idempotency
			if (eventId != null) {
				processedEventRepository.save(new ProcessedEvent(eventId, EVENT_TYPE, Instant.now()));
			}

			logger.info("Successfully processed order: eventId={}, orderId={}", eventId, message.orderId());
		}
		catch (Exception e) {
			logger.error("Failed to process order: eventId={}, orderId={}", eventId, message.orderId(), e);
			eventPublisher.publishStockFailedEvent(message.orderId(), e.getMessage());
		}
	}

}
