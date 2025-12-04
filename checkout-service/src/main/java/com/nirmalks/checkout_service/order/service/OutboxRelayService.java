package com.nirmalks.checkout_service.order.service;

import com.nirmalks.checkout_service.order.entity.Outbox;
import com.nirmalks.checkout_service.order.messaging.OrderEventPublisher;
import com.nirmalks.checkout_service.order.repository.OutboxRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutboxRelayService {
    private static final Logger logger = LoggerFactory.getLogger(OutboxRelayService.class);

    private final OutboxRepository outboxRepository;
    private final OrderEventPublisher orderEventPublisher;

    @Value("${outbox.batch-size:10}")
    private int batchSize;

    public OutboxRelayService(OutboxRepository outboxRepository, OrderEventPublisher orderEventPublisher) {
        this.outboxRepository = outboxRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void processOutboxEvents() {
        List<Outbox> events = outboxRepository.findByStatusOrderByCreatedAtAsc(
                Outbox.EventStatus.PENDING,
                PageRequest.of(0, batchSize)
        );

        if (events.isEmpty()) {
            return;
        }
        logger.info("Found {} pending outbox events. Processing...", events.size());

        for (Outbox event: events) {
            try{
                logger.info("Publishing event ID: {}", event.getId());
                orderEventPublisher.publishOrderCreatedEvent(event.getPayload());
                event.setStatus(Outbox.EventStatus.SENT);
            }
			catch (Exception e) {
                logger.error("Failed to process outbox event ID: {}", event.getId(), e);
                event.setStatus(Outbox.EventStatus.FAILED);
			}
        }
    }

}
