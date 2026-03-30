package com.nirmalks.checkout_service;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = { "user-service.base-url=http://localhost:8081",
		"catalog-service.base-url=http://localhost:8082", "rabbitmq.exchange.name=bookstore.exchange",
		"rabbitmq.routing.key=order.created", "rabbitmq.queue.name=order.queue" })
class CheckoutServiceApplicationTests {

	@MockitoBean
	private RedissonClient redissonClient;

	@Test
	void contextLoads() {
	}

}
