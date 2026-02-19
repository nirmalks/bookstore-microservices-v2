package com.nirmalks.audit_service.audit.service;

import com.nirmalks.audit_service.audit.entity.AuditLog;
import com.nirmalks.audit_service.audit.repository.AuditLogRepository;
import jakarta.transaction.Transactional;
import logging.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditIngestionService {

	private final AuditLogRepository repository;

	@Transactional
	public void ingest(AuditEvent event) {
		try {
			AuditLog auditLog = new AuditLog();
			auditLog.setEventId(event.eventId());
			auditLog.setSchemaVersion(event.schemaVersion());
			auditLog.setOccurredAt(event.occurredAt());
			auditLog.setServiceName(event.serviceName());
			auditLog.setEnvironment(event.environment());
			auditLog.setAction(event.action());
			auditLog.setResource(event.resource());
			auditLog.setResourceId(event.resourceId());
			auditLog.setStatus(event.status());
			auditLog.setPrincipal(event.principal());
			auditLog.setTraceId(event.traceId());
			auditLog.setSpanId(event.spanId());
			auditLog.setIdempotencyKey(event.idempotencyKey());
			auditLog.setDetail(event.detail());
			auditLog.setErrorCode(event.errorCode());
			auditLog.setErrorMessage(event.errorMessage());

			repository.save(auditLog);
		}
		catch (DataIntegrityViolationException ex) {
			// Duplicate insert due to redelivery/retry; safe to ignore.
			log.warn("Duplicate audit event ignored [eventId={}, occurredAt={}, key={}]", event.eventId(),
					event.occurredAt(), event.idempotencyKey());
		}
	}

}
