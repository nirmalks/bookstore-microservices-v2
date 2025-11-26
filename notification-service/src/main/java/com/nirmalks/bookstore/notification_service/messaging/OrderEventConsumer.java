package com.nirmalks.bookstore.notification_service.messaging;

import com.nirmalks.bookstore.notification_service.service.NotificationService;
import dto.OrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class OrderEventConsumer {

	private NotificationService notificationService;

	OrderEventConsumer(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	private Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

	@RabbitListener(queues = RabbitMqConfig.QUEUE_EMAIL)
	public void consumeOrderEvent(OrderMessage message) {
		logger.info("Received order event: {}", message);
		notificationService.sendEmail(message.orderId(), message.email());
	}

}
