package com.nirmalks.audit_service.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

@Configuration
public class RabbitConfig {

	@Bean
	public MessageConverter messageConverter() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		return new Jackson2JsonMessageConverter(mapper);
	}

	@Bean
	public Declarables auditExchangeBindings() {
		Queue auditQueue = new Queue("audit.queue", true);
		DirectExchange auditExchange = new DirectExchange("audit.exchange", true, false);
		Binding auditBinding = BindingBuilder.bind(auditQueue).to(auditExchange).with("audit.event");
		return new Declarables(auditQueue, auditExchange, auditBinding);
	}
}
