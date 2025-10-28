package com.nirmalks.checkout_service.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class OrderAsyncService {
    private final Logger logger = LoggerFactory.getLogger(OrderAsyncService.class);
    @Async("orderExecutor")
    public void sendEmail(String orderId, String userEmail) {
        logger.info("Sending email for order {} to {} on thread {}", orderId, userEmail, Thread.currentThread().getName());
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        logger.info("Email sent for {}", orderId);
    }

    @Async("orderExecutor")
    public void updateAnalytics(String orderId, double totalCost) {
        logger.info("Updating analytics for {} on thread {}", orderId, Thread.currentThread().getName());
    }

    @Async("orderExecutor")
    public void logAudit(String orderId, Long userId) {
        logger.info("Writing audit log for {} by user {} on thread {}", orderId, userId, Thread.currentThread().getName());
    }
}
