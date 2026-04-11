package com.nirmalks.checkout_service;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import locking.DistributedLockService;
import locking.RedissonConfig;

@SpringBootApplication
@Import({ RedissonConfig.class, DistributedLockService.class })
@EnableDiscoveryClient
@EnableAsync
@EnableRabbit
@EnableScheduling
public class CheckoutServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CheckoutServiceApplication.class, args);
	}

}
