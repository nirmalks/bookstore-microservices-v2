package com.nirmalks.bookstore.notification_service.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

	public static final String CHECKOUT_EXCHANGE = "checkout.topic";

	public static final String CHECKOUT_DLQ_EXCHANGE = "checkout.dlx";

	public static final String QUEUE_EMAIL = "checkout.email.queue";

	public static final String QUEUE_DEAD_LETTER = "checkout.dead.letter.queue";

	public static final String ORDER_ROUTING_KEY = "order.#";

	public static final String CHECKOUT_DLQ_ROUTING_KEY = "dead.letter";

	@Bean
	public TopicExchange checkoutExchange() {
		return ExchangeBuilder.topicExchange(CHECKOUT_EXCHANGE).durable(true).build();
	}

	@Bean
	public DirectExchange dlqExchange() {
		return ExchangeBuilder.directExchange(CHECKOUT_DLQ_EXCHANGE).durable(true).build();
	}

	@Bean
	public Queue emailQueue() {
		return QueueBuilder.durable(QUEUE_EMAIL)
			.withArgument("x-dead-letter-exchange", CHECKOUT_DLQ_EXCHANGE)
			.withArgument("x-dead-letter-routing-key", CHECKOUT_DLQ_ROUTING_KEY)
			.build();
	}

	@Bean
	public Queue deadLetterQueue() {
		return QueueBuilder.durable(QUEUE_DEAD_LETTER).build();
	}

	@Bean
	public Binding emailBinding() {
		return BindingBuilder.bind(emailQueue()).to(checkoutExchange()).with(ORDER_ROUTING_KEY);
	}

	@Bean
	public Binding dlqBinding() {
		return BindingBuilder.bind(deadLetterQueue()).to(dlqExchange()).with(CHECKOUT_DLQ_ROUTING_KEY);
	}

	@Bean
	public Jackson2JsonMessageConverter messageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(messageConverter());
		template.setObservationEnabled(true);
		return template;
	}

	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(messageConverter());
		factory.setObservationEnabled(true);
		return factory;
	}

}
