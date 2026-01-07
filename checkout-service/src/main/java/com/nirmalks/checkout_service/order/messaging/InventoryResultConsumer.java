package com.nirmalks.checkout_service.order.messaging;

import com.nirmalks.checkout_service.config.RabbitMqConfig;
import com.nirmalks.checkout_service.idempotency.entity.ProcessedEvent;
import com.nirmalks.checkout_service.idempotency.repository.ProcessedEventRepository;
import com.nirmalks.checkout_service.order.entity.OrderStatus;
import com.nirmalks.checkout_service.order.service.OrderService;
import dto.StockReservationFailedEvent;
import dto.StockReservationSuccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RabbitListener(queues = RabbitMqConfig.ORDER_INVENTORY_RESULT_QUEUE)
public class InventoryResultConsumer {

	private static final String EVENT_TYPE_SUCCESS = "STOCK_RESERVATION_SUCCESS";

	private static final String EVENT_TYPE_FAILED = "STOCK_RESERVATION_FAILED";

	private final OrderService orderService;

	private final ProcessedEventRepository processedEventRepository;

	private final Logger logger = LoggerFactory.getLogger(InventoryResultConsumer.class);

	public InventoryResultConsumer(OrderService orderService, ProcessedEventRepository processedEventRepository) {
		this.orderService = orderService;
		this.processedEventRepository = processedEventRepository;
	}

	@RabbitHandler
	@Transactional
	public void handleSuccess(StockReservationSuccessEvent event) {
		String eventId = event.eventId();

		// skip processing if event is already processed to ensure consumer idempotency
		if (eventId != null && processedEventRepository.existsByEventId(eventId)) {
			logger.info("Duplicate success event detected, skipping. eventId={}, orderId={}", eventId, event.orderId());
			return;
		}

		logger.info("Processing stock reservation success event: eventId={}, orderId={}", eventId, event.orderId());
		orderService.updateOrderStatusByEvent(event.orderId(), OrderStatus.CONFIRMED, "Stock confirmed.");

		// mark event as processed to ensure consumer idempotency
		if (eventId != null) {
			processedEventRepository.save(new ProcessedEvent(eventId, EVENT_TYPE_SUCCESS, Instant.now()));
		}
	}

	@RabbitHandler
	@Transactional
	public void handleFailure(StockReservationFailedEvent event) {
		String eventId = event.eventId();

		// skip processing if event is already processed to ensure consumer idempotency
		if (eventId != null && processedEventRepository.existsByEventId(eventId)) {
			logger.info("Duplicate failure event detected, skipping. eventId={}, orderId={}", eventId, event.orderId());
			return;
		}

		logger.info("Processing stock reservation failed event: eventId={}, orderId={}", eventId, event.orderId());
		orderService.updateOrderStatusByEvent(event.orderId(), OrderStatus.CANCELLED,
				"Stock reservation failed: " + event.reason());

		// mark event as processed to ensure consumer idempotency
		if (eventId != null) {
			processedEventRepository.save(new ProcessedEvent(eventId, EVENT_TYPE_FAILED, Instant.now()));
		}
	}

}
