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

	private final Counter inventoryEventsReceivedCounter;

	private final Counter inventoryDuplicateEventsSkippedCounter;

	private final Counter inventoryCompensationCounter;

	private final Counter inventoryPublishSuccessCounter;

	private final Counter inventoryPublishFailureCounter;

	private final Timer bookQueryTimer;

	private final Timer inventoryProcessingTimer;

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

		this.inventoryEventsReceivedCounter = Counter.builder("bookstore.catalog.events.received")
			.description("Inventory order-created events received by the catalog consumer")
			.register(meterRegistry);

		this.inventoryDuplicateEventsSkippedCounter = Counter.builder("bookstore.catalog.events.duplicates.skipped")
			.description("Duplicate inventory events skipped by idempotency checks")
			.register(meterRegistry);

		this.inventoryCompensationCounter = Counter.builder("bookstore.catalog.inventory.compensation.count")
			.description("Inventory compensations triggered after partial reservation work")
			.register(meterRegistry);

		this.inventoryPublishSuccessCounter = Counter.builder("bookstore.catalog.inventory.publish.success")
			.description("Inventory result events published successfully")
			.register(meterRegistry);

		this.inventoryPublishFailureCounter = Counter.builder("bookstore.catalog.inventory.publish.failed")
			.description("Inventory result events that failed to publish")
			.register(meterRegistry);

		this.bookQueryTimer = Timer.builder("books.query.time")
			.description("Time taken to query books")
			.tag("service", "catalog-service")
			.register(meterRegistry);

		this.inventoryProcessingTimer = Timer.builder("bookstore.catalog.inventory.processing.time")
			.description("Time taken by the inventory order-created consumer to process an event")
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

	public void incrementInventoryEventsReceived() {
		inventoryEventsReceivedCounter.increment();
	}

	public void incrementInventoryDuplicateEventsSkipped() {
		inventoryDuplicateEventsSkippedCounter.increment();
	}

	public void incrementInventoryCompensations() {
		inventoryCompensationCounter.increment();
	}

	public void incrementInventoryPublishSuccess() {
		inventoryPublishSuccessCounter.increment();
	}

	public void incrementInventoryPublishFailure() {
		inventoryPublishFailureCounter.increment();
	}

	public Timer.Sample startBookQueryTimer() {
		return Timer.start(meterRegistry);
	}

	public void stopBookQueryTimer(Timer.Sample sample) {
		sample.stop(bookQueryTimer);
	}

	public Timer.Sample startInventoryProcessingTimer() {
		return Timer.start(meterRegistry);
	}

	public void stopInventoryProcessingTimer(Timer.Sample sample) {
		sample.stop(inventoryProcessingTimer);
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
