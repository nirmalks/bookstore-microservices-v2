package com.nirmalks.checkout_service;

import com.nirmalks.checkout_service.metrics.OutboxMetrics;
import com.nirmalks.checkout_service.order.entity.Outbox;
import com.nirmalks.checkout_service.order.messaging.OrderEventPublisher;
import com.nirmalks.checkout_service.order.repository.OutboxRepository;
import com.nirmalks.checkout_service.order.service.OutboxRelayService;
import dto.OrderItemPayload;
import dto.OrderMessage;
import io.micrometer.core.instrument.Timer;
import locking.DistributedLockService;
import locking.LockKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {

	@Mock
	private OutboxRepository outboxRepository;

	@Mock
	private OrderEventPublisher orderEventPublisher;

	@Mock
	private DistributedLockService distributedLockService;

	@Mock
	private TransactionTemplate transactionTemplate;

	@Mock
	private OutboxMetrics outboxMetrics;

	@Mock
	private Timer.Sample timerSample;

	@InjectMocks
	private OutboxRelayService outboxRelayService;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(outboxRelayService, "batchSize", 10);
	}

	@Test
	void processOutboxEvents_recordsMetricsWhenEventsAreSent() {
		Outbox event = new Outbox("order-1", "ORDER_CREATED", new OrderMessage(null, "saga-1", "order-1", 1L,
				"user@example.com", 10.0, LocalDateTime.now(), List.of(new OrderItemPayload(1L, 2))));
		event.setId(42L);

		when(distributedLockService.tryExecuteWithLock(eq(LockKeys.OUTBOX_RELAY), eq(0L), eq(60L), eq(TimeUnit.SECONDS),
				any(Runnable.class)))
			.thenAnswer(invocation -> {
				Runnable runnable = invocation.getArgument(4);
				runnable.run();
				return true;
			});
		when(outboxMetrics.startProcessingTimer()).thenReturn(timerSample);
		doAnswer(invocation -> {
			Consumer<Object> callback = invocation.getArgument(0);
			callback.accept(null);
			return null;
		}).when(transactionTemplate).executeWithoutResult(any());
		when(outboxRepository.findByStatusOrderByCreatedAtAsc(eq(Outbox.EventStatus.PENDING), any(Pageable.class)))
			.thenReturn(List.of(event));

		outboxRelayService.processOutboxEvents();

		verify(outboxMetrics).incrementRuns();
		verify(outboxMetrics).recordBatchSize(1);
		verify(outboxMetrics).incrementEventsSent();
		verify(outboxMetrics).stopProcessingTimer(timerSample);
		verify(orderEventPublisher).publishOrderCreatedEvent(any(OrderMessage.class));
		verify(outboxRepository).saveAll(List.of(event));
	}

	@Test
	void processOutboxEvents_recordsLockSkipWhenAnotherInstanceOwnsTheRelay() {
		when(distributedLockService.tryExecuteWithLock(eq(LockKeys.OUTBOX_RELAY), eq(0L), eq(60L), eq(TimeUnit.SECONDS),
				any(Runnable.class)))
			.thenReturn(false);

		outboxRelayService.processOutboxEvents();

		verify(outboxMetrics).incrementLockSkipped();
		verify(outboxMetrics, never()).startProcessingTimer();
		verify(outboxRepository, never()).findByStatusOrderByCreatedAtAsc(any(), any(Pageable.class));
	}

}
