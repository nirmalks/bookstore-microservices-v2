package com.nirmalks.catalog_service.messaging;

import com.nirmalks.catalog_service.book.entity.StockReservation;
import com.nirmalks.catalog_service.book.repository.StockReservationRepository;
import com.nirmalks.catalog_service.book.service.BookService;
import com.nirmalks.catalog_service.idempotency.entity.ProcessedEvent;
import com.nirmalks.catalog_service.idempotency.repository.ProcessedEventRepository;
import com.rabbitmq.client.Channel;

import dto.OrderItemPayload;
import dto.OrderMessage;
import dto.StockReservationFailedEvent;
import dto.StockReservationSuccessEvent;
import exceptions.InsufficientStockException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderCreatedEventConsumer {

	private final Logger logger = LoggerFactory.getLogger(OrderCreatedEventConsumer.class);

	private final BookService bookService;

	private final InventoryEventPublisher eventPublisher;

	private final ProcessedEventRepository processedEventRepository;

	private final StockReservationRepository stockReservationRepository;

	OrderCreatedEventConsumer(BookService bookService, InventoryEventPublisher eventPublisher,
			ProcessedEventRepository processedEventRepository, StockReservationRepository stockReservationRepository) {
		this.bookService = bookService;
		this.eventPublisher = eventPublisher;
		this.processedEventRepository = processedEventRepository;
		this.stockReservationRepository = stockReservationRepository;
	}

	@RabbitListener(queues = RabbitMqConfig.QUEUE_INVENTORY)
	@Transactional
	public void consumeOrderCreatedEvent(OrderMessage message, Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
		String eventId = message.eventId();
		String sagaId = message.sagaId();

		// skip processing if event is already processed to ensure consumer idempotency
		if (eventId != null && processedEventRepository.existsByEventId(eventId)) {
			logger.info("Duplicate event detected, skipping. sagaId={}, orderId={}", sagaId, message.orderId());
			return;
		}

		logger.info("Processing order event for saga: sagaId={}, orderId={}", sagaId, message.orderId());

		List<StockReservationSuccessEvent.ReservedItem> reservedItems = new ArrayList<>();
		List<StockReservationFailedEvent.FailedItem> failedItems = new ArrayList<>();

		try {
			for (OrderItemPayload item : message.items()) {
				try {
					bookService.reserveStock(item.bookId(), item.quantity());
					reservedItems.add(new StockReservationSuccessEvent.ReservedItem(item.bookId(), item.quantity()));
				}
				catch (InsufficientStockException e) {
					failedItems.add(new StockReservationFailedEvent.FailedItem(item.bookId(), item.quantity(),
							e.getAvailableQuantity()));
				}
			}
			if (failedItems.isEmpty()) {
				// All items reserved successfully
				saveStockReservation(sagaId, message.orderId(), reservedItems);
				eventPublisher.publishStockReservedEvent(sagaId, message.orderId(), reservedItems);
				// mark event as processed to ensure consumer idempotency
				if (eventId != null) {
					processedEventRepository.save(new ProcessedEvent(eventId, "ORDER_CREATED", Instant.now()));
				}

				logger.info("Stock reserved successfully for saga: {}", sagaId);
			}
			else {
				// Compensate already reserved items
				for (StockReservationSuccessEvent.ReservedItem reserved : reservedItems) {
					bookService.releaseStock(reserved.bookId(), reserved.quantity());
				}

				eventPublisher.publishStockFailedEvent(sagaId, message.orderId(), "Insufficient stock for some items",
						failedItems);

				logger.warn("Stock reservation failed for saga: {}. Failed items: {}", sagaId, failedItems);
			}

		}
		catch (Exception e) {
			logger.error("Error processing order for saga: {}", sagaId, e);

			// Compensate any reserved items
			for (StockReservationSuccessEvent.ReservedItem reserved : reservedItems) {
				try {
					bookService.releaseStock(reserved.bookId(), reserved.quantity());
				}
				catch (Exception compensationError) {
					logger.error("Compensation failed for book {}: {}", reserved.bookId(),
							compensationError.getMessage());
				}
			}

			eventPublisher.publishStockFailedEvent(sagaId, message.orderId(), e.getMessage(), List.of());
		}
	}

	private void saveStockReservation(String sagaId, String orderId,
			List<StockReservationSuccessEvent.ReservedItem> items) {
		List<StockReservation.ReservedItem> entityItems = items.stream()
			.map(item -> new StockReservation.ReservedItem(item.bookId(), item.quantity()))
			.toList();
		StockReservation reservation = new StockReservation(sagaId, orderId, entityItems);
		stockReservationRepository.save(reservation);
	}

}
