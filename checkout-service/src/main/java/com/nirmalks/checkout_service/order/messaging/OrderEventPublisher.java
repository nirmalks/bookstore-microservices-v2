package com.nirmalks.checkout_service.order.messaging;

import com.nirmalks.checkout_service.config.RabbitMqConfig;

import dto.OrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventPublisher {

	private final RabbitTemplate rabbitTemplate;

	private final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);

	private String checkoutExchange = RabbitMqConfig.CHECKOUT_EXCHANGE;

	private String orderCreatedRoutingKey = RabbitMqConfig.ORDER_CREATED_ROUTING_KEY;

	public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public void publishOrderCreatedEvent(OrderMessage message) {
		logger.info("Publishing order event: {}", message);
		rabbitTemplate.convertAndSend(checkoutExchange, orderCreatedRoutingKey, message);
	}

}
