package com.nirmalks.audit_service.audit.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class AuditLogId implements Serializable {

	private Long id;

	private Instant occurredAt;

	public AuditLogId() {
	}

	public AuditLogId(Long id, Instant occurredAt) {
		this.id = id;
		this.occurredAt = occurredAt;
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

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		AuditLogId that = (AuditLogId) o;
		return Objects.equals(id, that.id) && Objects.equals(occurredAt, that.occurredAt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, occurredAt);
	}

}
