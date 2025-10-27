package com.nirmalks.checkout_service.order.service;

import com.nirmalks.checkout_service.order.dto.OrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class OrderEventConsumer {
    private OrderAsyncService orderAsyncService;

    OrderEventConsumer(OrderAsyncService orderAsyncService) {
        this.orderAsyncService = orderAsyncService;
    }
    private Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);
    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void consumeOrderEvent(OrderMessage message) {
        logger.info("Received order event: {}" , message);
        orderAsyncService.sendEmail(message.orderId(), message.email());
        orderAsyncService.updateAnalytics(message.orderId(), message.totalCost());
        orderAsyncService.logAudit(message.orderId(), message.userId());
    }
}
