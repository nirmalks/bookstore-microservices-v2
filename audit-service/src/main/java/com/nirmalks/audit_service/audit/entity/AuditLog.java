package com.nirmalks.audit_service.audit.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_log",
		indexes = { @Index(name = "idx_audit_occurred_at", columnList = "occurred_at"),
				@Index(name = "idx_audit_principal", columnList = "principal"),
				@Index(name = "idx_audit_action", columnList = "action"),
				@Index(name = "idx_audit_resource", columnList = "resource"),
				@Index(name = "idx_audit_trace", columnList = "trace_id"),
				@Index(name = "uk_audit_idempotency", columnList = "occurred_at, idempotency_key", unique = true) })
@IdClass(AuditLogId.class)
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_id_seq")
	@SequenceGenerator(name = "audit_log_id_seq", sequenceName = "audit_log_id_seq", allocationSize = 1)
	private Long id;

	@Id
	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Column(name = "event_id", nullable = false, length = 64)
	private String eventId;

	@Column(name = "schema_version", nullable = false, length = 16)
	private String schemaVersion;

	@Column(name = "service_name", nullable = false, length = 80)
	private String serviceName;

	@Column(name = "environment", length = 32)
	private String environment;

	@Column(name = "action", nullable = false, length = 80)
	private String action;

	@Column(name = "resource", nullable = false, length = 80)
	private String resource;

	@Column(name = "resource_id", length = 128)
	private String resourceId;

	@Column(name = "status", nullable = false, length = 16)
	private String status;

	@Column(name = "principal", length = 120)
	private String principal;

	@Column(name = "trace_id", length = 64)
	private String traceId;

	@Column(name = "span_id", length = 64)
	private String spanId;

	@Column(name = "idempotency_key", nullable = false, length = 300)
	private String idempotencyKey;

	@Column(name = "detail", length = 500)
	private String detail;

	@Column(name = "error_code", length = 120)
	private String errorCode;

	@Column(name = "error_message", length = 1000)
	private String errorMessage;

	@Column(name = "created_at")
	private Instant createdAt;

	@PrePersist
	public void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(Instant occurredAt) {
		this.occurredAt = occurredAt;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public String getSchemaVersion() {
		return schemaVersion;
	}

	public void setSchemaVersion(String schemaVersion) {
		this.schemaVersion = schemaVersion;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPrincipal() {
		return principal;
	}

	public void setPrincipal(String principal) {
		this.principal = principal;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public String getSpanId() {
		return spanId;
	}

	public void setSpanId(String spanId) {
		this.spanId = spanId;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public void setIdempotencyKey(String idempotencyKey) {
		this.idempotencyKey = idempotencyKey;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

}
