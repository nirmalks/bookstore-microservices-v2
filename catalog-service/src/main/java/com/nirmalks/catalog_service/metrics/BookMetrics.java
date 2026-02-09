package com.nirmalks.catalog_service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Business metrics for catalog operations Tracks book views, stock reservations, and
 * inventory operations
 */
@Component
public class BookMetrics {

	private final Counter bookViewsCounter;

	private final Counter stockReservationSuccessCounter;

	private final Counter stockReservationFailureCounter;

	private final Timer bookQueryTimer;

	private final MeterRegistry meterRegistry;

	public BookMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;

		this.bookViewsCounter = Counter.builder("books.views")
			.description("Total number of book views")
			.tag("service", "catalog-service")
			.register(meterRegistry);

		this.stockReservationSuccessCounter = Counter.builder("stock.reservation.success")
			.description("Successful stock reservations")
			.tag("service", "catalog-service")
			.register(meterRegistry);

		this.stockReservationFailureCounter = Counter.builder("stock.reservation.failure")
			.description("Failed stock reservations")
			.tag("service", "catalog-service")
			.register(meterRegistry);

		this.bookQueryTimer = Timer.builder("books.query.time")
			.description("Time taken to query books")
			.tag("service", "catalog-service")
			.register(meterRegistry);
	}

	public void incrementBookViews() {
		bookViewsCounter.increment();
	}

	public void incrementStockReservationSuccess() {
		stockReservationSuccessCounter.increment();
	}

	public void incrementStockReservationFailure() {
		stockReservationFailureCounter.increment();
	}

	public Timer.Sample startBookQueryTimer() {
		return Timer.start(meterRegistry);
	}

	public void stopBookQueryTimer(Timer.Sample sample) {
		sample.stop(bookQueryTimer);
	}

	public void recordLowStockAlert(Long bookId, int quantity) {
		Counter.builder("inventory.low.stock.alert")
			.description("Alert when inventory is low")
			.tag("service", "catalog-service")
			.tag("bookId", String.valueOf(bookId))
			.register(meterRegistry)
			.increment();
	}

	public void incrementStockReleased() {
		Counter.builder("stock.released")
			.description("Stock released due to order cancellation or saga compensation")
			.tag("service", "catalog-service")
			.register(meterRegistry)
			.increment();
	}

}
