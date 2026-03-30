package com.nirmalks.catalog_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.redisson.api.RedissonClient;

@SpringBootTest
class CatalogServiceApplicationTests {

	@MockitoBean
	private RedissonClient redissonClient;

	@Test
	void contextLoads() {
	}

}
