package com.nirmalks.checkout_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = { "user-service.base-url=http://localhost:8081",
		"catalog-service.base-url=http://localhost:8082", "rabbitmq.exchange.name=bookstore.exchange",
		"rabbitmq.routing.key=order.created", "rabbitmq.queue.name=order.queue" })
class CheckoutServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
