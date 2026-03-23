package com.nirmalks.checkout_service.order.service;

import com.nirmalks.checkout_service.metrics.OutboxMetrics;
import com.nirmalks.checkout_service.order.entity.Outbox;
import com.nirmalks.checkout_service.order.messaging.OrderEventPublisher;
import com.nirmalks.checkout_service.order.repository.OutboxRepository;
import dto.OrderMessage;
import io.micrometer.core.instrument.Timer;
import locking.DistributedLockService;
import locking.LockKeys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OutboxRelayService {

	private static final Logger logger = LoggerFactory.getLogger(OutboxRelayService.class);

	private final OutboxRepository outboxRepository;

	private final OrderEventPublisher orderEventPublisher;

	private final DistributedLockService distributedLockService;

	private final TransactionTemplate transactionTemplate;

	private final OutboxMetrics outboxMetrics;

	@Value("${outbox.batch-size:10}")
	private int batchSize;

	public OutboxRelayService(OutboxRepository outboxRepository, OrderEventPublisher orderEventPublisher,
			DistributedLockService distributedLockService, TransactionTemplate transactionTemplate,
			OutboxMetrics outboxMetrics) {
		this.outboxRepository = outboxRepository;
		this.orderEventPublisher = orderEventPublisher;
		this.distributedLockService = distributedLockService;
		this.transactionTemplate = transactionTemplate;
		this.outboxMetrics = outboxMetrics;
	}

	@Scheduled(fixedDelay = 2000)
	public void processOutboxEvents() {
		boolean executed = distributedLockService.tryExecuteWithLock(LockKeys.OUTBOX_RELAY, 0, // waitTime
				60, // hold lock for max 60 seconds
				TimeUnit.SECONDS, () -> {
					// Ensure transaction is committed BEFORE releasing the lock
					transactionTemplate.executeWithoutResult(status -> doProcessOutboxEvents());
				});

		if (!executed) {
			outboxMetrics.incrementLockSkipped();
			logger.debug("Outbox processing skipped - another instance is processing");
		}
	}

	private void doProcessOutboxEvents() {
		Timer.Sample sample = outboxMetrics.startProcessingTimer();
		List<Outbox> events = outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.EventStatus.PENDING,
				PageRequest.of(0, batchSize));

		if (events.isEmpty()) {
			outboxMetrics.stopProcessingTimer(sample);
			return;
		}

		try {
			outboxMetrics.incrementRuns();
			outboxMetrics.recordBatchSize(events.size());
			logger.info("Found {} pending outbox events. Processing...", events.size());

			for (Outbox event : events) {
				try {
					logger.info("Publishing event ID: {}", event.getId());
					// Inject the Outbox ID as the eventId for consumer idempotency
					OrderMessage originalMessage = event.getPayload();
					OrderMessage messageWithEventId = new OrderMessage(event.getId().toString(),
							originalMessage.sagaId(), originalMessage.orderId(), originalMessage.userId(),
							originalMessage.email(), originalMessage.totalCost(), originalMessage.placedAt(),
							originalMessage.items());

					orderEventPublisher.publishOrderCreatedEvent(messageWithEventId);
					event.setStatus(Outbox.EventStatus.SENT);
					outboxMetrics.incrementEventsSent();
				}
				catch (Exception e) {
					logger.error("Failed to process outbox event ID: {}", event.getId(), e);
					event.setStatus(Outbox.EventStatus.FAILED);
					outboxMetrics.incrementEventsFailed();
				}
			}
			outboxRepository.saveAll(events);
		}
		finally {
			outboxMetrics.stopProcessingTimer(sample);
		}
	}

}
