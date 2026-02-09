package com.nirmalks.checkout_service.saga;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.nirmalks.checkout_service.order.repository.OrderRepository;

import jakarta.transaction.Transactional;
import saga.SagaState;

@Service
public class SagaStateManager {

	private static final Logger logger = LoggerFactory.getLogger(SagaStateManager.class);

	private final OrderRepository orderRepository;

	private final SagaMetrics sagaMetrics;

	public SagaStateManager(OrderRepository orderRepository, SagaMetrics sagaMetrics) {
		this.orderRepository = orderRepository;
		this.sagaMetrics = sagaMetrics;
	}

	@Transactional
	public void updateSagaState(String sagaId, SagaState newState) {
		orderRepository.findBySagaId(sagaId).ifPresentOrElse(order -> {
			SagaState previousState = order.getSagaState();

			if (!isValidStateTransition(previousState, newState)) {
				logger.warn("Invalid saga state transition from {} to {} for saga {}", previousState, newState, sagaId);
				return;
			}

			order.setSagaState(newState);
			orderRepository.save(order);

			logger.info("Saga {} state updated: {} -> {}", sagaId, previousState, newState);
			sagaMetrics.recordStateTransition(previousState.name(), newState.name());
		}, () -> logger.error("Saga not found: {}", sagaId));
	}

	@Transactional
	public void completeSaga(String sagaId) {
		orderRepository.findBySagaId(sagaId).ifPresent(order -> {
			order.setSagaState(SagaState.SAGA_COMPLETED);
			order.setSagaCompletedAt(LocalDateTime.now());
			orderRepository.save(order);

			sagaMetrics.incrementSagasCompleted();
			sagaMetrics.recordSagaDuration(order.getSagaStartedAt(), order.getSagaCompletedAt());

			logger.info("Saga completed: {}", sagaId);
		});
	}

	@Transactional
	public void completeSagaWithCompensation(String sagaId, String reason) {
		orderRepository.findBySagaId(sagaId).ifPresent(order -> {
			order.setSagaState(SagaState.SAGA_COMPENSATION_COMPLETED);
			order.setSagaCompletedAt(LocalDateTime.now());
			order.setCompensationReason(reason);
			orderRepository.save(order);

			sagaMetrics.incrementSagasCompensated();

			logger.info("Saga compensation completed: {}. Reason: {}", sagaId, reason);
		});
	}

	private boolean isValidStateTransition(SagaState from, SagaState to) {
		return switch (from) {
			case SAGA_STARTED -> to == SagaState.STOCK_RESERVATION_PENDING;
			case STOCK_RESERVATION_PENDING ->
				to == SagaState.STOCK_RESERVED || to == SagaState.STOCK_RESERVATION_FAILED;
			case STOCK_RESERVED -> to == SagaState.SAGA_COMPLETED || to == SagaState.SAGA_COMPENSATING;
			case STOCK_RESERVATION_FAILED -> to == SagaState.SAGA_COMPENSATING;
			case SAGA_COMPENSATING -> to == SagaState.SAGA_COMPENSATION_COMPLETED || to == SagaState.SAGA_FAILED;
			default -> false;
		};
	}

	/**
	 * Check if a saga is in a terminal state (completed or compensated).
	 * @param sagaId the saga identifier
	 * @return true if the saga is completed or compensation completed
	 */
	public boolean isSagaCompleted(String sagaId) {
		return orderRepository.findBySagaId(sagaId).map(order -> {
			SagaState state = order.getSagaState();
			return state == SagaState.SAGA_COMPLETED || state == SagaState.SAGA_COMPENSATION_COMPLETED
					|| state == SagaState.SAGA_FAILED;
		}).orElse(false);
	}

}