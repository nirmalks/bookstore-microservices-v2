package com.nirmalks.checkout_service.idempotency.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nirmalks.checkout_service.idempotency.entity.ProcessedEvent;

import java.time.Instant;

/**
 * Repository for managing processed event records. Used to check for duplicate events and
 * record successfully processed events.
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

	/**
	 * Check if an event has already been processed.
	 * @param eventId the unique event identifier
	 * @return true if the event has been processed, false otherwise
	 */
	boolean existsByEventId(String eventId);

	/**
	 * Delete old processed events to prevent unbounded table growth. Events older than
	 * the specified cutoff time will be removed.
	 * @param cutoffTime events processed before this time will be deleted
	 * @return number of deleted records
	 */
	long deleteByProcessedAtBefore(Instant cutoffTime);

}
