package com.nirmalks.checkout_service.common;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncTasksService {
    @Async
    public void sendEmail(String orderId, String userEmail) {
        System.out.println("Sending email for order " + orderId +
                " to " + userEmail +
                " on thread " + Thread.currentThread().getName());
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        System.out.println("Email sent for " + orderId);
    }

    @Async
    public void updateAnalytics(String orderId, double totalCost) {
        System.out.println("Updating analytics for " + orderId +
                " on thread " + Thread.currentThread().getName());
    }

    @Async
    public void logAudit(String orderId, Long userId) {
        System.out.println("Writing audit log for " + orderId +
                " by user " + userId +
                " on thread " + Thread.currentThread().getName());
    }
}
