package com.nirmalks.bookstore.notification_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

	private final Logger logger = LoggerFactory.getLogger(NotificationService.class);

	@Async("notificationExecutor")
	public void sendEmail(String orderId, String userEmail) {
		logger.info("Sending email for order {} to {} on thread {}", orderId, userEmail,
				Thread.currentThread().getName());
		try {
			Thread.sleep(2000);
		}
		catch (InterruptedException e) {
		}
		logger.info("Email sent for {}", orderId);
	}

}
