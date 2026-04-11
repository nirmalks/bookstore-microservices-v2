package com.nirmalks.checkout_service;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest()
class CheckoutServiceApplicationTests {

	@MockitoBean
	private RedissonClient redissonClient;

	@Test
	void contextLoads() {
	}

}
