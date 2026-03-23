package com.nirmalks.bookstore.notification_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyCleanupMetrics {

	private final Counter cleanupRunsCounter;

	private final Counter cleanupFailuresCounter;

	private final Counter cleanupLockSkippedCounter;

	private final DistributionSummary rowsDeletedSummary;

	private final Timer cleanupTimer;

	private final MeterRegistry meterRegistry;

	public IdempotencyCleanupMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		this.cleanupRunsCounter = Counter.builder("bookstore.idempotency.cleanup.runs")
			.description("Number of processed-event cleanup runs executed")
			.register(meterRegistry);
		this.cleanupFailuresCounter = Counter.builder("bookstore.idempotency.cleanup.failed")
			.description("Number of processed-event cleanup runs that failed")
			.register(meterRegistry);
		this.cleanupLockSkippedCounter = Counter.builder("bookstore.idempotency.cleanup.lock.skipped")
			.description("Number of cleanup runs skipped because the lock was not acquired")
			.register(meterRegistry);
		this.rowsDeletedSummary = DistributionSummary.builder("bookstore.idempotency.cleanup.rows.deleted")
			.description("Number of processed-event rows deleted per cleanup run")
			.register(meterRegistry);
		this.cleanupTimer = Timer.builder("bookstore.idempotency.cleanup.time")
			.description("Time taken to execute a processed-event cleanup run")
			.register(meterRegistry);
	}

	public void incrementRuns() {
		cleanupRunsCounter.increment();
	}

	public void incrementFailures() {
		cleanupFailuresCounter.increment();
	}

	public void incrementLockSkipped() {
		cleanupLockSkippedCounter.increment();
	}

	public void recordRowsDeleted(long rowsDeleted) {
		rowsDeletedSummary.record(rowsDeleted);
	}

	public Timer.Sample startCleanupTimer() {
		return Timer.start(meterRegistry);
	}

	public void stopCleanupTimer(Timer.Sample sample) {
		sample.stop(cleanupTimer);
	}

}
