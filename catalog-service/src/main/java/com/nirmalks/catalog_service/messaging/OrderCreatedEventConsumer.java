package com.nirmalks.catalog_service.messaging;

import com.nirmalks.catalog_service.book.service.BookService;
import com.rabbitmq.client.Channel;
import dto.OrderMessage;
import dto.StockReservationFailedEvent;
import dto.StockReservationSuccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class OrderCreatedEventConsumer {
    private final Logger logger = LoggerFactory.getLogger(OrderCreatedEventConsumer.class);
    private final RabbitTemplate rabbitTemplate;

    private BookService bookService;
    OrderCreatedEventConsumer(BookService bookService, RabbitTemplate rabbitTemplate) {
        this.bookService = bookService;
        this.rabbitTemplate = rabbitTemplate;
    }
    @RabbitListener(queues = RabbitMqConfig.QUEUE_INVENTORY)
    public void consumeOrderCreatedEvent(OrderMessage message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        logger.info("Received order event to update inventory: {}", message);
        try {
            bookService.updateStock(message);
            StockReservationSuccessEvent successEvent = new StockReservationSuccessEvent(message.orderId());
            rabbitTemplate.convertAndSend(RabbitMqConfig.INVENTORY_EXCHANGE, RabbitMqConfig.STOCK_RESERVATION_SUCCESS_ROUTING_KEY, successEvent);
            logger.info("Successfully processed order: {}", message.orderId());
        } catch (Exception e) {
            var event = new StockReservationFailedEvent(message.orderId(), e.getMessage());
            logger.info("failed to process order: {}", message.orderId());
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.INVENTORY_EXCHANGE,
                    RabbitMqConfig.STOCK_RESERVATION_FAILED_ROUTING_KEY,
                    event
            );
        }

    }
}
