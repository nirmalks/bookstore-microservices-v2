package com.nirmalks.audit_service.audit.repository;

import com.nirmalks.audit_service.audit.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class AuditLogSpecification {

	private AuditLogSpecification() {
	}

	public static Specification<AuditLog> filter(String principal, String action, String resource, String status,
			String traceId, Instant from, Instant to) {
		return (root, query, cb) -> cb.and(
				principal == null ? cb.conjunction() : cb.equal(root.get("principal"), principal),
				action == null ? cb.conjunction() : cb.equal(root.get("action"), action),
				resource == null ? cb.conjunction() : cb.equal(root.get("resource"), resource),
				status == null ? cb.conjunction() : cb.equal(root.get("status"), status),
				traceId == null ? cb.conjunction() : cb.equal(root.get("traceId"), traceId),
				from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("occurredAt"), from),
				to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("occurredAt"), to));
	}

}
