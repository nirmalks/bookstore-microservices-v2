package com.nirmalks.checkout_service.saga.entity;

import jakarta.persistence.*;
import saga.SagaState;

import java.time.LocalDateTime;

@Entity
@Table(name = "saga_audit_log")
public class SagaAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "saga_id", nullable = false)
	private String sagaId;

	@Column(name = "order_id")
	private String orderId;

	@Column(name = "previous_state")
	@Enumerated(EnumType.STRING)
	private SagaState previousState;

	@Column(name = "new_state")
	@Enumerated(EnumType.STRING)
	private SagaState newState;

	@Column(name = "event_type")
	private String eventType;

	@Column(name = "source_service")
	private String sourceService;

	@Column(name = "message", length = 1000)
	private String message;

	@Column(name = "timestamp", nullable = false)
	private LocalDateTime timestamp = LocalDateTime.now();

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public void setSourceService(String sourceService) {
		this.sourceService = sourceService;
	}

	public void setSagaId(String sagaId) {
		this.sagaId = sagaId;
	}

	public void setPreviousState(SagaState previousState) {
		this.previousState = previousState;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public void setNewState(SagaState newState) {
		this.newState = newState;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSagaId() {
		return sagaId;
	}

	public String getOrderId() {
		return orderId;
	}

	public SagaState getPreviousState() {
		return previousState;
	}

	public SagaState getNewState() {
		return newState;
	}

	public String getEventType() {
		return eventType;
	}

	public String getSourceService() {
		return sourceService;
	}

	public String getMessage() {
		return message;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

}