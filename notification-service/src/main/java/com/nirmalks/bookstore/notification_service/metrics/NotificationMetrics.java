package com.nirmalks.bookstore.notification_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Business metrics for notification operations Tracks email notifications sent, failed,
 * duplicates skipped, and processing performance
 */
@Component
public class NotificationMetrics {

	private final Counter emailsSentCounter;

	private final Counter emailsFailedCounter;

	private final Counter duplicateEventsSkippedCounter;

	private final Counter eventsReceivedCounter;

	private final Timer emailProcessingTimer;

	private final MeterRegistry meterRegistry;

	private final ConcurrentHashMap<String, Counter> emailsByTypeCounters = new ConcurrentHashMap<>();

	public NotificationMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;

		this.emailsSentCounter = Counter.builder("notifications.emails.sent")
			.description("Total number of email notifications sent successfully")
			.tag("service", "notification-service")
			.register(meterRegistry);

		this.emailsFailedCounter = Counter.builder("notifications.emails.failed")
			.description("Total number of failed email notifications")
			.tag("service", "notification-service")
			.register(meterRegistry);

		this.duplicateEventsSkippedCounter = Counter.builder("notifications.events.duplicates.skipped")
			.description("Total number of duplicate events skipped via idempotency check")
			.tag("service", "notification-service")
			.register(meterRegistry);

		this.eventsReceivedCounter = Counter.builder("notifications.events.received")
			.description("Total number of order events received from RabbitMQ")
			.tag("service", "notification-service")
			.register(meterRegistry);

		this.emailProcessingTimer = Timer.builder("notifications.email.processing.time")
			.description("Time taken to process and send an email notification")
			.tag("service", "notification-service")
			.register(meterRegistry);
	}

	public void incrementEmailsSent() {
		emailsSentCounter.increment();
	}

	public void incrementEmailsFailed() {
		emailsFailedCounter.increment();
	}

	public void incrementDuplicateEventsSkipped() {
		duplicateEventsSkippedCounter.increment();
	}

	public void incrementEventsReceived() {
		eventsReceivedCounter.increment();
	}

	public Timer.Sample startEmailProcessingTimer() {
		return Timer.start(meterRegistry);
	}

	public void stopEmailProcessingTimer(Timer.Sample sample) {
		sample.stop(emailProcessingTimer);
	}

	public void recordEmailByType(String eventType) {
		emailsByTypeCounters
			.computeIfAbsent(eventType,
					t -> Counter.builder("notifications.emails.by.type")
						.description("Email notifications grouped by event type")
						.tag("type", t)
						.tag("service", "notification-service")
						.register(meterRegistry))
			.increment();
	}

}
