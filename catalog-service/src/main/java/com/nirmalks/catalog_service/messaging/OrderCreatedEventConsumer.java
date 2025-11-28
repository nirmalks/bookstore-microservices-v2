package com.nirmalks.catalog_service.messaging;

import com.nirmalks.catalog_service.book.service.BookService;
import com.rabbitmq.client.Channel;
import dto.OrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class OrderCreatedEventConsumer {
    private Logger logger = LoggerFactory.getLogger(OrderCreatedEventConsumer.class);

    private BookService bookService;
    OrderCreatedEventConsumer(BookService bookService) {
        this.bookService = bookService;
    }
    @RabbitListener(queues = RabbitMqConfig.QUEUE_INVENTORY)
    public void consumeOrderCreatedEvent(OrderMessage message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        logger.info("Received order event to update inventory: {}", message);
        bookService.updateStock(message);
        logger.info("Successfully processed and ACKed order: {}", message.orderId());
    }
}
