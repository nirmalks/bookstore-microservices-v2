package com.nirmalks.checkout_service.order.messaging;

import com.nirmalks.checkout_service.config.RabbitMqConfig;
import com.nirmalks.checkout_service.idempotency.entity.ProcessedEvent;
import com.nirmalks.checkout_service.idempotency.repository.ProcessedEventRepository;
import com.nirmalks.checkout_service.order.entity.OrderStatus;
import com.nirmalks.checkout_service.order.service.OrderService;
import com.nirmalks.checkout_service.saga.SagaStateManager;

import dto.StockReservationFailedEvent;
import dto.StockReservationSuccessEvent;
import saga.SagaState;

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

	private final SagaStateManager sagaStateManager;

	public InventoryResultConsumer(OrderService orderService, ProcessedEventRepository processedEventRepository,
			SagaStateManager sagaStateManager) {
		this.orderService = orderService;
		this.processedEventRepository = processedEventRepository;
		this.sagaStateManager = sagaStateManager;
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

		String sagaId = event.sagaId();

		logger.info("Processing stock reservation success for saga: {}", sagaId);
		sagaStateManager.updateSagaState(sagaId, SagaState.STOCK_RESERVED);
		orderService.updateOrderStatusByEvent(event.orderId(), OrderStatus.CONFIRMED, "Stock confirmed.");
		sagaStateManager.completeSaga(sagaId);
		// mark event as processed to ensure consumer idempotency
		if (eventId != null) {
			processedEventRepository.save(new ProcessedEvent(eventId, EVENT_TYPE_SUCCESS, Instant.now()));
		}
		logger.info("Saga completed successfully: {}", sagaId);
	}

	@RabbitHandler
	@Transactional
	public void handleFailure(StockReservationFailedEvent event) {
		String eventId = event.eventId();
		String sagaId = event.sagaId();

		// skip processing if event is already processed to ensure consumer idempotency
		if (eventId != null && processedEventRepository.existsByEventId(eventId)) {
			logger.info("Duplicate failure event detected, skipping. eventId={}, orderId={}", eventId, event.orderId());
			return;
		}

		// Skip if saga is already completed or compensated (handles multiple failure
		// events for same saga)
		if (sagaStateManager.isSagaCompleted(sagaId)) {
			logger.debug("Saga {} already completed/compensated, skipping duplicate failure event", sagaId);
			return;
		}

		logger.info("Processing stock reservation failure for saga: {}. Reason: {}", sagaId, event.reason());
		// Proper state machine transitions for failure:
		// STOCK_RESERVATION_PENDING -> STOCK_RESERVATION_FAILED -> SAGA_COMPENSATING ->
		// SAGA_COMPENSATION_COMPLETED
		sagaStateManager.updateSagaState(sagaId, SagaState.STOCK_RESERVATION_FAILED);
		sagaStateManager.updateSagaState(sagaId, SagaState.SAGA_COMPENSATING);
		orderService.updateOrderStatusByEvent(event.orderId(), OrderStatus.CANCELLED,
				"Stock reservation failed: " + event.reason());
		sagaStateManager.completeSagaWithCompensation(sagaId, event.reason());
		// mark event as processed to ensure consumer idempotency
		if (eventId != null) {
			processedEventRepository.save(new ProcessedEvent(eventId, EVENT_TYPE_FAILED, Instant.now()));
		}
		logger.info("Saga compensation completed: {}", sagaId);
	}

}
