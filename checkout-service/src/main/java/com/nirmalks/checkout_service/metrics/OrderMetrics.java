package com.nirmalks.checkout_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Business metrics for order operations Tracks order creation, failures, revenue, and
 * performance
 */
@Component
public class OrderMetrics {

	private final Counter ordersCreatedCounter;

	private final Counter ordersFailedCounter;

	private final Counter ordersCancelledCounter;

	private final Timer orderCreationTimer;

	private final MeterRegistry meterRegistry;

	private final ConcurrentHashMap<String, Counter> ordersByStatusCounters = new ConcurrentHashMap<>();

	public OrderMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;

		// Counters for order lifecycle
		this.ordersCreatedCounter = Counter.builder("orders.created")
			.description("Total number of orders created")
			.tag("service", "checkout-service")
			.register(meterRegistry);

		this.ordersFailedCounter = Counter.builder("orders.failed")
			.description("Total number of failed orders")
			.tag("service", "checkout-service")
			.register(meterRegistry);

		this.ordersCancelledCounter = Counter.builder("orders.cancelled")
			.description("Total number of cancelled orders")
			.tag("service", "checkout-service")
			.register(meterRegistry);

		// Timer for performance tracking
		this.orderCreationTimer = Timer.builder("orders.creation.time")
			.description("Time taken to create an order")
			.tag("service", "checkout-service")
			.register(meterRegistry);
	}

	public void incrementOrdersCreated() {
		ordersCreatedCounter.increment();
	}

	public void incrementOrdersFailed() {
		ordersFailedCounter.increment();
	}

	public void incrementOrdersCancelled() {
		ordersCancelledCounter.increment();
	}

	public void recordOrderCreationTime(long milliseconds) {
		orderCreationTimer.record(java.time.Duration.ofMillis(milliseconds));
	}

	public Timer.Sample startOrderCreationTimer() {
		return Timer.start(meterRegistry);
	}

	public void stopOrderCreationTimer(Timer.Sample sample) {
		sample.stop(orderCreationTimer);
	}

	public void recordOrderByStatus(String status) {
		ordersByStatusCounters
			.computeIfAbsent(status,
					s -> Counter.builder("orders.by.status")
						.description("Orders grouped by status")
						.tag("status", s)
						.tag("service", "checkout-service")
						.register(meterRegistry))
			.increment();
	}

	public void recordRevenue(double amount) {
		Counter.builder("orders.revenue")
			.description("Total revenue from orders")
			.tag("service", "checkout-service")
			.register(meterRegistry)
			.increment(amount);
	}

}
