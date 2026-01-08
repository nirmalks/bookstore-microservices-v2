package com.nirmalks.checkout_service.idempotency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entity to track processed event IDs for consumer idempotency. When a message is
 * successfully processed, its eventId is stored here. Before processing any message, we
 * check if its eventId already exists to prevent duplicate processing.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

	@Id
	@Column(name = "event_id", nullable = false, length = 255)
	private String eventId;

	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	@Column(name = "processed_at", nullable = false)
	private Instant processedAt;

	public ProcessedEvent() {
	}

	public ProcessedEvent(String eventId, String eventType, Instant processedAt) {
		this.eventId = eventId;
		this.eventType = eventType;
		this.processedAt = processedAt;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public Instant getProcessedAt() {
		return processedAt;
	}

	public void setProcessedAt(Instant processedAt) {
		this.processedAt = processedAt;
	}

}
