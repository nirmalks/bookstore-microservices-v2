package com.nirmalks.checkout_service.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.nirmalks.checkout_service.order.entity.Order;
import com.nirmalks.checkout_service.order.repository.OrderRepository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import saga.SagaState;

@ExtendWith(MockitoExtension.class)
class SagaStateManagerTest {

	@InjectMocks
	private SagaStateManager sagaStateManager;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private SagaMetrics sagaMetrics;

	private ListAppender<ILoggingEvent> listAppender;

	@BeforeEach
	void setup() {
		Logger logger = (Logger) LoggerFactory.getLogger(SagaStateManager.class);
		listAppender = new ListAppender<>();
		listAppender.start();
		logger.addAppender(listAppender);
	}

	@AfterEach
	void tearDown() {
		Logger logger = (Logger) LoggerFactory.getLogger(SagaStateManager.class);
		logger.detachAppender(listAppender);
	}

	@Test
	void updateSagaState_should_log_error_when_saga_not_found() {
		when(orderRepository.findBySagaId(any())).thenReturn(Optional.empty());
		sagaStateManager.updateSagaState("sageId", SagaState.SAGA_STARTED);

		assertThat(listAppender.list).extracting(ILoggingEvent::getFormattedMessage).contains("Saga not found: sageId");
		assertThat(listAppender.list).extracting(ILoggingEvent::getLevel).contains(Level.ERROR);
	}

	@Test
	void updateSagaState_should_log_warning_when_invalid_transition() {
		Order order = new Order();
		order.setSagaState(SagaState.SAGA_COMPLETED);
		when(orderRepository.findBySagaId(any())).thenReturn(Optional.of(order));
		sagaStateManager.updateSagaState("sageId", SagaState.SAGA_STARTED);

		assertThat(listAppender.list).extracting(ILoggingEvent::getFormattedMessage)
			.contains("Invalid saga state transition from SAGA_COMPLETED to SAGA_STARTED for saga sageId");
		assertThat(listAppender.list).extracting(ILoggingEvent::getLevel).contains(Level.WARN);
	}

	@Test
	void updateSagaState_should_update_state_and_log_info_for_valid_transition() {
		Order order = new Order();
		order.setSagaState(SagaState.SAGA_STARTED);
		when(orderRepository.findBySagaId(any())).thenReturn(Optional.of(order));
		sagaStateManager.updateSagaState("sageId", SagaState.STOCK_RESERVATION_PENDING);

		assertThat(listAppender.list).extracting(ILoggingEvent::getFormattedMessage)
			.contains("Saga sageId state updated: SAGA_STARTED -> STOCK_RESERVATION_PENDING");
		assertThat(listAppender.list).extracting(ILoggingEvent::getLevel).contains(Level.INFO);
		verify(orderRepository).save(order);
		verify(sagaMetrics).recordStateTransition(SagaState.SAGA_STARTED.name(),
				SagaState.STOCK_RESERVATION_PENDING.name());
	}

	@Test
	void completeSaga_should_update_state_and_log_info() {
		Order order = new Order();
		order.setSagaState(SagaState.SAGA_STARTED);
		when(orderRepository.findBySagaId(any())).thenReturn(Optional.of(order));
		sagaStateManager.completeSaga("sageId");

		assertThat(listAppender.list).extracting(ILoggingEvent::getFormattedMessage).contains("Saga completed: sageId");
		assertThat(listAppender.list).extracting(ILoggingEvent::getLevel).contains(Level.INFO);
		verify(orderRepository).save(order);
		verify(sagaMetrics).incrementSagasCompleted();
		verify(sagaMetrics).recordSagaDuration(order.getSagaStartedAt(), order.getSagaCompletedAt());
	}

	@Test
	void completeSagaWithCompensation_should_update_state_and_log_info() {
		Order order = new Order();
		order.setSagaState(SagaState.SAGA_STARTED);
		when(orderRepository.findBySagaId(any())).thenReturn(Optional.of(order));
		sagaStateManager.completeSagaWithCompensation("sageId", "reason");

		assertThat(listAppender.list).extracting(ILoggingEvent::getFormattedMessage)
			.contains("Saga compensation completed: sageId. Reason: reason");
		assertThat(listAppender.list).extracting(ILoggingEvent::getLevel).contains(Level.INFO);
		verify(orderRepository).save(order);
		verify(sagaMetrics).incrementSagasCompensated();
	}

	@Test
	void isSagaCompleted_should_return_true_when_saga_is_completed() {
		Order order = new Order();
		order.setSagaState(SagaState.SAGA_COMPLETED);
		when(orderRepository.findBySagaId(any())).thenReturn(Optional.of(order));
		assertThat(sagaStateManager.isSagaCompleted("sageId")).isTrue();
	}

	@Test
	void isSagaCompleted_should_return_true_when_saga_is_compensated() {
		Order order = new Order();
		order.setSagaState(SagaState.SAGA_COMPENSATION_COMPLETED);
		when(orderRepository.findBySagaId(any())).thenReturn(Optional.of(order));
		assertThat(sagaStateManager.isSagaCompleted("sageId")).isTrue();
	}

	@Test
	void isSagaCompleted_should_return_true_when_saga_is_failed() {
		Order order = new Order();
		order.setSagaState(SagaState.SAGA_FAILED);
		when(orderRepository.findBySagaId(any())).thenReturn(Optional.of(order));
		assertThat(sagaStateManager.isSagaCompleted("sageId")).isTrue();
	}

	@Test
	void isSagaCompleted_should_return_false_when_saga_is_not_completed() {
		Order order = new Order();
		order.setSagaState(SagaState.SAGA_STARTED);
		when(orderRepository.findBySagaId(any())).thenReturn(Optional.of(order));
		assertThat(sagaStateManager.isSagaCompleted("sageId")).isFalse();
	}

	@Test
	void isSagaCompleted_should_return_false_when_saga_not_found() {
		when(orderRepository.findBySagaId(any())).thenReturn(Optional.empty());
		assertThat(sagaStateManager.isSagaCompleted("sageId")).isFalse();
	}

}
