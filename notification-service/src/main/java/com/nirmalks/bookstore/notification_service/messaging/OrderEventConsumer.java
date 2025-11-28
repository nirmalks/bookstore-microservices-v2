package com.nirmalks.bookstore.notification_service.messaging;

import com.nirmalks.bookstore.notification_service.service.NotificationService;
import com.rabbitmq.client.Channel;
import dto.OrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class OrderEventConsumer {

	private NotificationService notificationService;

	OrderEventConsumer(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	private Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

	@RabbitListener(queues = RabbitMqConfig.QUEUE_EMAIL)
	public void consumeOrderEvent(OrderMessage message, Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
		logger.info("Received order event: {}", message);

		try {
			notificationService.sendEmail(message.orderId(), message.email());
			channel.basicAck(deliveryTag, false);
			logger.info("Successfully handed off email task for order: {} and ACKed message.", message.orderId());

		}
		catch (Exception e) {
			logger.error("Error processing order for email: {}", message.orderId(), e);
			try {
				channel.basicNack(deliveryTag, false, false);
			}
			catch (Exception nackE) {
				logger.error("Failed during NACK process: {}", nackE.getMessage());
			}
		}
	}

}
