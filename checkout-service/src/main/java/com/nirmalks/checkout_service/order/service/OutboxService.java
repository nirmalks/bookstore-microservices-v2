package com.nirmalks.checkout_service.order.service;

import com.nirmalks.checkout_service.order.entity.Outbox;
import com.nirmalks.checkout_service.order.repository.OutboxRepository;
import dto.OrderMessage;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {
    private final OutboxRepository outboxRepository;
    private final Logger logger = LoggerFactory.getLogger(OutboxService.class);
    public OutboxService(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void saveOrderCreatedEvent(String orderIdString, OrderMessage message) {
        logger.info("Outbox event save is triggered from order creation");
        Outbox outboxEvent = new Outbox(
                orderIdString,
                "OrderCreated",
                message
        );
        outboxRepository.save(outboxEvent);
    }
}
