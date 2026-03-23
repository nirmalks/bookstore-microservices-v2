package com.nirmalks.checkout_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class OutboxMetrics {

	private final Counter outboxEventsSentCounter;

	private final Counter outboxEventsFailedCounter;

	private final Counter outboxLockSkippedCounter;

	private final Counter outboxRunsCounter;

	private final DistributionSummary outboxBatchSizeSummary;

	private final Timer outboxProcessingTimer;

	private final MeterRegistry meterRegistry;

	public OutboxMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		this.outboxEventsSentCounter = Counter.builder("bookstore.outbox.events.sent")
			.description("Total number of outbox events published successfully")
			.register(meterRegistry);
		this.outboxEventsFailedCounter = Counter.builder("bookstore.outbox.events.failed")
			.description("Total number of outbox events that failed to publish")
			.register(meterRegistry);
		this.outboxLockSkippedCounter = Counter.builder("bookstore.outbox.lock.skipped")
			.description("Number of outbox relay executions skipped because the lock was not acquired")
			.register(meterRegistry);
		this.outboxRunsCounter = Counter.builder("bookstore.outbox.runs")
			.description("Number of outbox relay runs that processed a batch")
			.register(meterRegistry);
		this.outboxBatchSizeSummary = DistributionSummary.builder("bookstore.outbox.batch.size")
			.description("Number of events picked up in each outbox relay batch")
			.register(meterRegistry);
		this.outboxProcessingTimer = Timer.builder("bookstore.outbox.processing.time")
			.description("Time taken to process an outbox relay batch")
			.register(meterRegistry);
	}

	public void incrementEventsSent() {
		outboxEventsSentCounter.increment();
	}

	public void incrementEventsFailed() {
		outboxEventsFailedCounter.increment();
	}

	public void incrementLockSkipped() {
		outboxLockSkippedCounter.increment();
	}

	public void incrementRuns() {
		outboxRunsCounter.increment();
	}

	public void recordBatchSize(int batchSize) {
		outboxBatchSizeSummary.record(batchSize);
	}

	public Timer.Sample startProcessingTimer() {
		return Timer.start(meterRegistry);
	}

	public void stopProcessingTimer(Timer.Sample sample) {
		sample.stop(outboxProcessingTimer);
	}

}
