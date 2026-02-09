package com.nirmalks.checkout_service.saga;

import com.nirmalks.checkout_service.order.entity.Order;
import com.nirmalks.checkout_service.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saga.SagaState;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SagaTimeoutHandler {

	private static final Logger logger = LoggerFactory.getLogger(SagaTimeoutHandler.class);

	@Value("${saga.timeout.minutes:5}")
	private int sagaTimeoutMinutes;

	private final OrderRepository orderRepository;

	private final OrderCancellationHandler cancellationHandler;

	public SagaTimeoutHandler(OrderRepository orderRepository, OrderCancellationHandler cancellationHandler) {
		this.orderRepository = orderRepository;
		this.cancellationHandler = cancellationHandler;
	}

	@Scheduled(fixedDelay = 60000) // Run every minute
	@Transactional
	public void handleTimedOutSagas() {
		LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(sagaTimeoutMinutes);

		List<Order> timedOutOrders = orderRepository.findByPendingSagaStatesBefore(
				List.of(SagaState.STOCK_RESERVATION_PENDING, SagaState.SAGA_COMPENSATING), timeoutThreshold);

		for (Order order : timedOutOrders) {
			try {
				logger.warn("Saga timeout detected for order {}: state={}, started={}", order.getId(),
						order.getSagaState(), order.getSagaStartedAt());

				cancellationHandler.cancelOrder(order.getId(),
						"Saga timeout: no response received within " + sagaTimeoutMinutes + " minutes");

			}
			catch (Exception e) {
				logger.error("Failed to handle saga timeout for order {}", order.getId(), e);
			}
		}
	}

}